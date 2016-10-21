package act.handler.builtin.controller.impl;

import act.Act;
import act.ActComponent;
import act.app.ActionContext;
import act.app.App;
import act.app.AppClassLoader;
import act.controller.Controller;
import act.controller.meta.*;
import act.handler.builtin.controller.*;
import act.inject.param.JsonDTO;
import act.inject.param.JsonDTOClassManager;
import act.inject.param.ParamValueLoaderManager;
import act.inject.param.ParamValueLoaderService;
import act.security.CORS;
import act.security.CSRF;
import act.util.DestroyableBase;
import act.view.ActNotFound;
import act.view.Template;
import act.view.TemplatePathResolver;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.inject.BeanSpec;
import org.osgl.mvc.annotation.SessionFree;
import org.osgl.mvc.result.BadRequest;
import org.osgl.mvc.result.NotFound;
import org.osgl.mvc.result.Result;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implement handler using
 * https://github.com/EsotericSoftware/reflectasm
 */
public class ReflectedHandlerInvoker<M extends HandlerMethodMetaInfo> extends DestroyableBase
        implements ActionHandlerInvoker, AfterInterceptorInvoker, ExceptionInterceptorInvoker {

    private static final Object[] DUMP_PARAMS = new Object[0];
    private ClassLoader cl;
    private ControllerClassMetaInfo controller;
    private Class<?> controllerClass;
    private MethodAccess methodAccess;
    private M handler;
    private int handlerIndex;
    private Map<H.Format, Boolean> templateCache = C.newMap();
    protected Method method; //
    private ParamValueLoaderService paramLoaderService;
    private JsonDTOClassManager jsonDTOClassManager;
    private final int paramCount;
    private final int fieldsAndParamsCount;
    private String singleJsonFieldName;
    private final boolean sessionFree;
    private List<BeanSpec> paramSpecs;
    private Set<String> pathVariables;
    private CORS.Spec corsSpec;
    private CSRF.Spec csrfSpec;

    private ReflectedHandlerInvoker(M handlerMetaInfo, App app) {
        this.cl = app.classLoader();
        this.handler = handlerMetaInfo;
        this.controller = handlerMetaInfo.classInfo();
        this.controllerClass = $.classForName(controller.className(), cl);
        this.paramLoaderService = app.service(ParamValueLoaderManager.class).get(ActionContext.class);
        this.jsonDTOClassManager = app.service(JsonDTOClassManager.class);

        Class[] paramTypes = paramTypes(cl);
        try {
            method = controllerClass.getMethod(handlerMetaInfo.name(), paramTypes);
        } catch (NoSuchMethodException e) {
            throw E.unexpected(e);
        }
        if (!handlerMetaInfo.isStatic()) {
            //constructorAccess = ConstructorAccess.get(controllerClass);
            methodAccess = MethodAccess.get(controllerClass);
            handlerIndex = methodAccess.getIndex(handlerMetaInfo.name(), paramTypes);
        } else {
            method.setAccessible(true);
        }

        sessionFree = method.isAnnotationPresent(SessionFree.class);

        paramCount = handler.paramCount();
        paramSpecs = jsonDTOClassManager.beanSpecs(controllerClass, method);
        fieldsAndParamsCount = paramSpecs.size();
        if (fieldsAndParamsCount == 1) {
            singleJsonFieldName = paramSpecs.get(0).name();
        }

        CORS.Spec corsSpec = CORS.spec(method).chain(CORS.spec(controllerClass));
        this.corsSpec = corsSpec;

        CSRF.Spec csrfSpec = CSRF.spec(method).chain(CSRF.spec(controllerClass));
        this.csrfSpec = csrfSpec;
    }

    @Override
    protected void releaseResources() {
        cl = null;
        controller = null;
        controllerClass = null;
        method = null;
        methodAccess = null;
        handler.destroy();
        handler = null;
        super.releaseResources();
    }

    @Override
    public int priority() {
        return handler.priority();
    }

    public interface ReflectedHandlerInvokerVisitor extends Visitor, $.Func2<Class<?>, Method, Void> {
    }

    @Override
    public void accept(Visitor visitor) {
        ReflectedHandlerInvokerVisitor rv = (ReflectedHandlerInvokerVisitor) visitor;
        rv.apply(controllerClass, method);
    }

    public Result handle(ActionContext actionContext) throws Exception {
        ensureJsonDTOGenerated(actionContext);
        Object ctrl = controllerInstance(actionContext);
        Object[] params = params(actionContext);
        return invoke(handler, actionContext, ctrl, params);
    }

    @Override
    public Result handle(Result result, ActionContext actionContext) throws Exception {
        actionContext.attribute(ActionContext.ATTR_RESULT, result);
        return handle(actionContext);
    }

    @Override
    public Result handle(Exception e, ActionContext actionContext) throws Exception {
        actionContext.attribute(ActionContext.ATTR_EXCEPTION, e);
        return handle(actionContext);
    }

    @Override
    public boolean sessionFree() {
        return sessionFree;
    }

    public CORS.Spec corsSpec() {
        return corsSpec;
    }

    @Override
    public CSRF.Spec csrfSpec() {
        return csrfSpec;
    }

    private void ensureJsonDTOGenerated(ActionContext context) {
        if (0 == fieldsAndParamsCount || !context.jsonEncoded() || null != context.attribute(JsonDTO.CTX_KEY)) {
            return;
        }
        Class<? extends JsonDTO> dtoClass = jsonDTOClassManager.get(controllerClass, method);
        if (null == dtoClass) {
            // there are neither fields nor params
            return;
        }
        try {
            JsonDTO dto = JSON.parseObject(patchedJsonBody(context), dtoClass);
            context.attribute(JsonDTO.CTX_KEY, dto);
        } catch (JSONException e) {
            if (e.getCause() != null) {
                App.logger.warn(e.getCause(), "error parsing JSON data");
            } else {
                App.logger.warn(e, "error parsing JSON data");
            }
            throw new BadRequest(e.getCause());
        }
    }

    private int fieldsAndParamsCount(ActionContext context) {
        if (fieldsAndParamsCount < 2) {
            return fieldsAndParamsCount;
        }
        return fieldsAndParamsCount - pathVariables(context).size();
    }

    private Set<String> pathVariables(ActionContext context) {
        if (null == pathVariables) {
            pathVariables = context.attribute(ActionContext.ATTR_PATH_VARS);
        }
        return pathVariables;
    }

    private String singleJsonFieldName(ActionContext context) {
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

    /**
     * Suppose method signature is: `public void foo(Foo foo)`, and a JSON content is
     * not `{"foo": {foo-content}}`, then wrap it as `{"foo": body}`
     */
    private String patchedJsonBody(ActionContext context) {
        String body = context.body();
        if (S.blank(body) || 1 < fieldsAndParamsCount(context)) {
            return body;
        }
        String theName = singleJsonFieldName(context);
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
                    if (theName.charAt(i - nameStart - 1) != c) {
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

    private Class[] paramTypes(ClassLoader cl) {
        int sz = handler.paramCount();
        Class[] ca = new Class[sz];
        for (int i = 0; i < sz; ++i) {
            HandlerParamMetaInfo param = handler.param(i);
            ca[i] = $.classForName(param.type().getClassName(), cl);
        }
        return ca;
    }

    private Object controllerInstance(ActionContext context) {
        String controllerName = controllerClass.getName();
        Object inst = context.__controllerInstance(controllerName);
        if (null == inst) {
            inst = paramLoaderService.loadHostBean(controllerClass, context);
            context.__controllerInstance(controllerName, inst);
        }
        return inst;
    }

    private Result invoke(M handlerMetaInfo, ActionContext context, Object controller, Object[] params) throws Exception {
        Object result;
        if (null != methodAccess) {
            try {
                result = methodAccess.invoke(controller, handlerIndex, params);
            } catch (Result r) {
                return r;
            }
        } else {
            try {
                result = method.invoke(null, params);
            } catch (Result r) {
                return r;
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Result) {
                    return (Result) cause;
                }
                throw (Exception) cause;
            }
        }
        if (null == result && handler.hasReturn() && !handler.returnTypeInfo().isResult()) {
            // ActFramework respond 404 Not Found when
            // handler invoker return `null`
            // and there are return type of the action method signature
            // and the return type is **NOT** Result
            return ActNotFound.create(method);
        }
        boolean hasTemplate = checkTemplate(context);
        return Controller.Util.inferResult(handlerMetaInfo, result, context, hasTemplate);
    }

    private synchronized boolean checkTemplate(ActionContext context) {
        if (!context.state().isHandling()) {
            // we don't check template on interceptors
            return false;
        }
        H.Format fmt = context.accept();
        Boolean hasTemplate = templateCache.get(fmt);
        if (null == hasTemplate || Act.isDev()) {
            if (!TemplatePathResolver.isAcceptFormatSupported(fmt)) {
                hasTemplate = false;
            } else {
                Template t = Act.viewManager().load(context);
                hasTemplate = t != null;
            }
            templateCache.put(fmt, hasTemplate);
        }
        return hasTemplate;
    }

    private Object[] params(ActionContext context) {
        if (0 == paramCount) {
            return DUMP_PARAMS;
        }
        return paramLoaderService.loadMethodParams(method, context);
    }

    public static ControllerAction createControllerAction(ActionMethodMetaInfo meta, App app) {
        return new ControllerAction(new ReflectedHandlerInvoker(meta, app));
    }

    public static BeforeInterceptor createBeforeInterceptor(InterceptorMethodMetaInfo meta, App app) {
        return new _Before(new ReflectedHandlerInvoker(meta, app));
    }

    public static AfterInterceptor createAfterInterceptor(InterceptorMethodMetaInfo meta, App app) {
        return new _After(new ReflectedHandlerInvoker(meta, app));
    }

    public static ExceptionInterceptor createExceptionInterceptor(CatchMethodMetaInfo meta, App app) {
        return new _Exception(new ReflectedHandlerInvoker(meta, app), meta);
    }

    public static FinallyInterceptor createFinannyInterceptor(InterceptorMethodMetaInfo meta, App app) {
        return new _Finally(new ReflectedHandlerInvoker(meta, app));
    }

    @ActComponent
    private static class _Before extends BeforeInterceptor {
        private ActionHandlerInvoker invoker;

        _Before(ActionHandlerInvoker invoker) {
            super(invoker.priority());
            this.invoker = invoker;
        }

        @Override
        public Result handle(ActionContext actionContext) throws Exception {
            return invoker.handle(actionContext);
        }

        @Override
        public boolean sessionFree() {
            return invoker.sessionFree();
        }

        @Override
        public void accept(Visitor visitor) {
            invoker.accept(visitor.invokerVisitor());
        }

        @Override
        public CORS.Spec corsSpec() {
            return invoker.corsSpec();
        }

        @Override
        protected void releaseResources() {
            invoker.destroy();
            invoker = null;
        }
    }

    @ActComponent
    private static class _After extends AfterInterceptor {
        private AfterInterceptorInvoker invoker;

        _After(AfterInterceptorInvoker invoker) {
            super(invoker.priority());
            this.invoker = invoker;
        }

        @Override
        public Result handle(Result result, ActionContext actionContext) throws Exception {
            return invoker.handle(result, actionContext);
        }

        @Override
        public CORS.Spec corsSpec() {
            return invoker.corsSpec();
        }

        @Override
        public boolean sessionFree() {
            return invoker.sessionFree();
        }

        @Override
        public void accept(Visitor visitor) {
            invoker.accept(visitor.invokerVisitor());
        }

        @Override
        public void accept(ActionHandlerInvoker.Visitor visitor) {
            invoker.accept(visitor);
        }

        @Override
        protected void releaseResources() {
            invoker.destroy();
            invoker = null;
        }
    }

    @ActComponent
    private static class _Exception extends ExceptionInterceptor {
        private ExceptionInterceptorInvoker invoker;

        _Exception(ExceptionInterceptorInvoker invoker, CatchMethodMetaInfo metaInfo) {
            super(invoker.priority(), exceptionClassesOf(metaInfo));
            this.invoker = invoker;
        }

        @SuppressWarnings("unchecked")
        private static List<Class<? extends Exception>> exceptionClassesOf(CatchMethodMetaInfo metaInfo) {
            List<String> classNames = metaInfo.exceptionClasses();
            List<Class<? extends Exception>> clsList;
            clsList = C.newSizedList(classNames.size());
            AppClassLoader cl = App.instance().classLoader();
            for (String cn : classNames) {
                clsList.add((Class) $.classForName(cn, cl));
            }
            return clsList;
        }

        @Override
        protected Result internalHandle(Exception e, ActionContext actionContext) throws Exception {
            return invoker.handle(e, actionContext);
        }

        @Override
        public boolean sessionFree() {
            return invoker.sessionFree();
        }

        @Override
        public void accept(ActionHandlerInvoker.Visitor visitor) {
            invoker.accept(visitor);
        }

        @Override
        public void accept(Visitor visitor) {
            invoker.accept(visitor.invokerVisitor());
        }

        @Override
        public CORS.Spec corsSpec() {
            return invoker.corsSpec();
        }

        @Override
        protected void releaseResources() {
            invoker.destroy();
            invoker = null;
        }
    }

    @ActComponent
    private static class _Finally extends FinallyInterceptor {
        private ActionHandlerInvoker invoker;

        _Finally(ActionHandlerInvoker invoker) {
            super(invoker.priority());
            this.invoker = invoker;
        }

        @Override
        public void handle(ActionContext actionContext) throws Exception {
            invoker.handle(actionContext);
        }

        @Override
        public CORS.Spec corsSpec() {
            return invoker.corsSpec();
        }

        @Override
        public boolean sessionFree() {
            return invoker.sessionFree();
        }

        @Override
        public void accept(Visitor visitor) {
            invoker.accept(visitor.invokerVisitor());
        }

        @Override
        protected void releaseResources() {
            invoker.destroy();
            invoker = null;
        }
    }

}
