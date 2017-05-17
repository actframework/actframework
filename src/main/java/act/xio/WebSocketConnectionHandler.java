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
import act.controller.meta.ActionMethodMetaInfo;
import act.controller.meta.ControllerClassMetaInfo;
import act.controller.meta.HandlerParamMetaInfo;
import act.handler.RequestHandlerBase;
import act.inject.param.*;
import act.sys.Env;
import act.ws.WebSocketConnectionManager;
import act.ws.WebSocketContext;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.mvc.annotation.WsAction;
import org.osgl.mvc.result.BadRequest;
import org.osgl.util.E;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public abstract class WebSocketConnectionHandler extends RequestHandlerBase {

    private static final Object[] DUMP_PARAMS = new Object[0];
    protected boolean disabled;
    protected ClassLoader cl;
    protected WebSocketConnectionManager connectionManager;
    protected ActionMethodMetaInfo handler;
    protected ControllerClassMetaInfo controller;
    protected Class<?> handlerClass;
    protected Method method;
    protected MethodAccess methodAccess;
    private int methodIndex;
    protected boolean isStatic;
    private ParamValueLoaderService paramLoaderService;
    private JsonDTOClassManager jsonDTOClassManager;
    private int paramCount;
    private int fieldsAndParamsCount;
    private String singleJsonFieldName;
    private String jsonDTOKey;
    private List<BeanSpec> paramSpecs;
    private Object host;
    private boolean isValid;
    private Class[] paramTypes;
    private boolean isSingleParam;


    public WebSocketConnectionHandler(ActionMethodMetaInfo methodInfo, WebSocketConnectionManager manager) {
        App app = manager.app();
        this.cl = app.classLoader();
        this.connectionManager = $.notNull(manager);
        this.handler = $.notNull(methodInfo);
        this.controller = handler.classInfo();

        this.paramLoaderService = app.service(ParamValueLoaderManager.class).get(WebSocketContext.class);
        this.jsonDTOClassManager = app.service(JsonDTOClassManager.class);

        this.handlerClass = $.classForName(controller.className(), cl);
        this.disabled = !Env.matches(handlerClass);
        this.jsonDTOKey = app.cuid();

        paramTypes = paramTypes(cl);

        try {
            this.method = handlerClass.getMethod(methodInfo.name(), paramTypes);
            this.isValid = null != this.method.getAnnotation(WsAction.class);
            this.disabled = this.disabled || !Env.matches(method);
        } catch (NoSuchMethodException e) {
            throw E.unexpected(e);
        }

        if (!isValid || disabled) {
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

        ParamValueLoader[] loaders = paramLoaderService.methodParamLoaders(host, method);
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

    public boolean isValid() {
        return isValid;
    }

    @Override
    public void prepareAuthentication(ActionContext context) {
    }

    protected void invoke(WebSocketContext context) {
        if (disabled) {
            return;
        }
        ensureJsonDTOGenerated(context);
        Object[] params = params(context);
        if (this.isStatic) {
            $.invokeStatic(method, params);
        } else {
            methodAccess.invoke(host, methodIndex, params);
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

    private Class[] paramTypes(ClassLoader cl) {
        int sz = handler.paramCount();
        Class[] ca = new Class[sz];
        for (int i = 0; i < sz; ++i) {
            HandlerParamMetaInfo param = handler.param(i);
            ca[i] = $.classForName(param.type().getClassName(), cl);
        }
        return ca;
    }

    private void ensureJsonDTOGenerated(WebSocketContext context) {
        if (0 == fieldsAndParamsCount || !context.isJson() || null != context.attribute(jsonDTOKey)) {
            return;
        }
        Class<? extends JsonDTO> dtoClass = jsonDTOClassManager.get(handlerClass, method);
        if (null == dtoClass) {
            // there are neither fields nor params
            return;
        }
        try {
            JsonDTO dto = JSON.parseObject(patchedJsonBody(context), dtoClass);
            context.attribute(jsonDTOKey, dto);
        } catch (JSONException e) {
            if (e.getCause() != null) {
                App.LOGGER.warn(e.getCause(), "error parsing JSON data");
            } else {
                App.LOGGER.warn(e, "error parsing JSON data");
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
        for (BeanSpec spec: paramSpecs) {
            String name = spec.name();
            if (!set.contains(name)) {
                return name;
            }
        }
        return null;
    }


}
