package act.xio;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.Act;
import act.app.ActionContext;
import act.app.App;
import act.app.event.*;
import act.controller.meta.*;
import act.event.SysEventListenerBase;
import act.handler.RequestHandlerBase;
import act.inject.param.*;
import act.sys.Env;
import act.view.ActBadRequest;
import act.ws.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.mvc.annotation.WsAction;
import org.osgl.mvc.result.BadRequest;
import org.osgl.util.*;

import java.lang.reflect.Method;
import java.util.*;

public abstract class WebSocketConnectionHandler extends RequestHandlerBase {

    private static final Object[] DUMP_PARAMS = new Object[0];
    protected boolean disabled;
    protected App app;
    protected WebSocketConnectionManager connectionManager;
    protected ActionMethodMetaInfo handler;
    protected ControllerClassMetaInfo controller;
    protected Class<?> handlerClass;
    protected Method method;
    protected MethodAccess methodAccess;
    private int methodIndex;
    protected boolean isStatic;
    private ParamValueLoaderService paramLoaderService;
    private JsonDtoClassManager jsonDTOClassManager;
    private int paramCount;
    private int fieldsAndParamsCount;
    private String singleJsonFieldName;
    private List<BeanSpec> paramSpecs;
    private Object host;
    private boolean isWsHandler;
    private Class[] paramTypes;
    private boolean isSingleParam;
    private WebSocketConnectionListener connectionListener;
    private WebSocketConnectionListener.Manager connectionListenerManager;

    // used to compose connection only websocket handler
    protected WebSocketConnectionHandler(WebSocketConnectionManager manager) {
        this.connectionManager = manager;
        this.isWsHandler = false;
        this.disabled = true;
        this.app = manager.app();
        this.initWebSocketConnectionListenerManager();
    }

    public WebSocketConnectionHandler(ActionMethodMetaInfo methodInfo, WebSocketConnectionManager manager) {
        this.connectionManager = $.requireNotNull(manager);
        this.app = manager.app();
        this.initWebSocketConnectionListenerManager();
        if (null == methodInfo) {
            this.isWsHandler = false;
            this.disabled = true;
            return;
        }
        this.handler = $.requireNotNull(methodInfo);
        this.controller = handler.classInfo();

        this.paramLoaderService = app.service(ParamValueLoaderManager.class).get(WebSocketContext.class);
        this.jsonDTOClassManager = app.service(JsonDtoClassManager.class);

        this.handlerClass = app.classForName(controller.className());
        this.disabled = !Env.matches(handlerClass);

        paramTypes = paramTypes(app);

        this.method = $.getMethod(handlerClass, methodInfo.name(), paramTypes);
        E.unexpectedIf(null == method, "Unable to locate handler method: " + methodInfo.name());
        this.isWsHandler = null != this.method.getAnnotation(WsAction.class);
        this.disabled = this.disabled || !Env.matches(method);

        if (!isWsHandler || disabled) {
            return;
        }

        this.isStatic = methodInfo.isStatic();
        if (!this.isStatic) {
            //constructorAccess = ConstructorAccess.get(controllerClass);
            methodAccess = MethodAccess.get(handlerClass);
            methodIndex = methodAccess.getIndex(methodInfo.name(), paramTypes);
            host = Act.getInstance(handlerClass);
        } else {
            method.setAccessible(true);
        }

        paramCount = handler.paramCount();
        paramSpecs = jsonDTOClassManager.beanSpecs(handlerClass, method);
        fieldsAndParamsCount = paramSpecs.size();
        if (fieldsAndParamsCount == 1) {
            singleJsonFieldName = paramSpecs.get(0).name();
        }
        // todo: do we want to allow inject Annotation type into web socket
        // handler method param list?
        ParamValueLoader[] loaders = paramLoaderService.methodParamLoaders(host, method, null);
        if (loaders.length > 0) {
            int realParamCnt = 0;
            for (ParamValueLoader loader : loaders) {
                if (loader instanceof ProvidedValueLoader) {
                    continue;
                }
                realParamCnt++;
            }
            isSingleParam = 1 == realParamCnt;
        }
    }

    @Override
    protected void releaseResources() {
        app = null;
        connectionManager = null;
        connectionListenerManager = null;
        handler = null;
        controller = null;
        handlerClass = null;
        method = null;
        methodAccess = null;
        paramLoaderService = null;
        jsonDTOClassManager = null;
        paramSpecs = null;
        host = null;
        $.resetArray(paramTypes);
        paramTypes = null;
        super.releaseResources();
    }

    private void initWebSocketConnectionListenerManager() {
        App app = Act.app();
        if (app.eventEmitted(SysEventId.DEPENDENCY_INJECTOR_PROVISIONED)) {
            this.connectionListenerManager = app.getInstance(WebSocketConnectionListener.Manager.class);
        } else {
            final WebSocketConnectionHandler me = this;
            this.app.eventBus().bind(SysEventId.DEPENDENCY_INJECTOR_PROVISIONED, new SysEventListenerBase() {
                @Override
                public void on(EventObject event) {
                    me.connectionListenerManager = me.app.getInstance(WebSocketConnectionListener.Manager.class);
                }
            });
        }
    }

    protected void setConnectionListener(WebSocketConnectionListener connectionListener) {
        this.connectionListener = $.requireNotNull(connectionListener);
    }

    /**
     * Called by implementation class once websocket connection established
     * at networking layer.
     *
     * @param context the websocket context
     */
    protected final void _onConnect(WebSocketContext context) {
        if (null != connectionListener) {
            connectionListener.onConnect(context);
        }
        connectionListenerManager.notifyFreeListeners(context, false);
        Act.eventBus().emit(new WebSocketConnectEvent(context));
    }

    protected final void _onClose(WebSocketContext context) {
        if (null != connectionListener) {
            connectionListener.onClose(context);
        }
        connectionListenerManager.notifyFreeListeners(context, true);
        Act.eventBus().emit(new WebSocketCloseEvent(context));
    }

    @Override
    public String toString() {
        return "websocket connection handler";
    }

    /**
     * This method is used by {@link act.handler.builtin.controller.RequestHandlerProxy}
     * to check if a handler is WS handler or GET handler
     *
     * @return `true` if this is a real WS handler
     */
    public boolean isWsHandler() {
        return isWsHandler;
    }

    @Override
    public void prepareAuthentication(ActionContext context) {
    }

    protected void invoke(WebSocketContext context) {
        if (disabled) {
            return;
        }
        ensureJsonDtoGenerated(context);
        Object[] params = params(context);
        Object retVal;
        if (this.isStatic) {
            retVal = $.invokeStatic(method, params);
        } else {
            retVal = methodAccess.invoke(host, methodIndex, params);
        }
        if (null == retVal) {
            return;
        }
        if (retVal instanceof String) {
            context.sendToSelf((String) retVal);
        } else {
            context.sendJsonToSelf(retVal);
        }
    }

    private Object[] params(WebSocketContext context) {
        if (0 == paramCount) {
            return DUMP_PARAMS;
        }
        Object[] params = paramLoaderService.loadMethodParams(host, method, context);
        if (isSingleParam) {
            for (int i = 0; i < paramCount; ++i) {
                if (null == params[i]) {
                    String singleVal = context.stringMessage();
                    Class<?> paramType = paramTypes[i];
                    StringValueResolver resolver = context.app().resolverManager().resolver(paramType);
                    if (null != resolver) {
                        params[i] = resolver.apply(singleVal);
                    } else {
                        E.unexpected("Cannot determine string value resolver for param type: %s", paramType);
                    }
                }
            }
        }
        return params;
    }

    private Class[] paramTypes(App app) {
        int sz = handler.paramCount();
        Class[] ca = new Class[sz];
        for (int i = 0; i < sz; ++i) {
            HandlerParamMetaInfo param = handler.param(i);
            ca[i] = app.classForName(param.type().getClassName());
        }
        return ca;
    }

    private void ensureJsonDtoGenerated(WebSocketContext context) {
        if (0 == fieldsAndParamsCount || !context.isJson()) {
            return;
        }
        Class<? extends JsonDto> dtoClass = jsonDTOClassManager.get(paramSpecs, handlerClass);
        if (null == dtoClass) {
            // there are neither fields nor params
            return;
        }
        try {
            JsonDto dto = JSON.parseObject(patchedJsonBody(context), dtoClass);
            context.attribute(JsonDto.CTX_ATTR_KEY, dto);
        } catch (JSONException e) {
            if (e.getCause() != null) {
                logger.warn(e.getCause(), "error parsing JSON data");
            } else {
                logger.warn(e, "error parsing JSON data");
            }
            throw new BadRequest(e.getCause());
        }
    }

    /**
     * Suppose method signature is: `public void foo(Foo foo)`, and a JSON content is
     * not `{"foo": {foo-content}}`, then wrap it as `{"foo": body}`
     */
    private String patchedJsonBody(WebSocketContext context) {
        String body = context.stringMessage();
        if (S.blank(body) || 1 < fieldsAndParamsCount) {
            return body;
        }
        String theName = singleJsonFieldName(context);
        int theNameLen = theName.length();
        if (null == theName) {
            return body;
        }
        body = body.trim();
        boolean needPatch = body.charAt(0) == '[';
        if (!needPatch) {
            if (body.charAt(0) != '{') {
                throw new IllegalArgumentException("Cannot parse JSON string: " + body);
            }
            boolean startCheckName = false;
            int nameStart = -1;
            for (int i = 1; i < body.length(); ++i) {
                char c = body.charAt(i);
                if (c == ' ') {
                    continue;
                }
                if (startCheckName) {
                    if (c == '"') {
                        break;
                    }
                    int id = i - nameStart - 1;
                    if (id >= theNameLen || theName.charAt(i - nameStart - 1) != c) {
                        needPatch = true;
                        break;
                    }
                } else if (c == '"') {
                    startCheckName = true;
                    nameStart = i;
                }
            }
        }
        return needPatch ? S.fmt("{\"%s\": %s}", theName, body) : body;
    }

    private String singleJsonFieldName(WebSocketContext context) {
        if (null != singleJsonFieldName) {
            return singleJsonFieldName;
        }
        Set<String> set = context.paramKeys();
        for (BeanSpec spec : paramSpecs) {
            String name = spec.name();
            if (!set.contains(name)) {
                return name;
            }
        }
        return null;
    }


}
