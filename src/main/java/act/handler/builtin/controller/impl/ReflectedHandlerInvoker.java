package act.handler.builtin.controller.impl;

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
import act.app.AppClassLoader;
import act.controller.CacheSupportMetaInfo;
import act.controller.Controller;
import act.controller.annotation.HandleCsrfFailure;
import act.controller.annotation.HandleMissingAuthentication;
import act.controller.annotation.TemplateContext;
import act.controller.meta.*;
import act.handler.NonBlock;
import act.handler.PreventDoubleSubmission;
import act.handler.builtin.controller.*;
import act.inject.DependencyInjector;
import act.inject.param.JsonDTO;
import act.inject.param.JsonDTOClassManager;
import act.inject.param.ParamValueLoaderManager;
import act.inject.param.ParamValueLoaderService;
import act.security.CORS;
import act.security.CSRF;
import act.sys.Env;
import act.util.*;
import act.view.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.inject.BeanSpec;
import org.osgl.mvc.annotation.ResponseContentType;
import org.osgl.mvc.annotation.ResponseStatus;
import org.osgl.mvc.annotation.SessionFree;
import org.osgl.mvc.result.BadRequest;
import org.osgl.mvc.result.Conflict;
import org.osgl.mvc.result.NotFound;
import org.osgl.mvc.result.Result;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static act.inject.param.JsonDTO.CTX_ATTR_KEY;

/**
 * Implement handler using
 * https://github.com/EsotericSoftware/reflectasm
 */
public class ReflectedHandlerInvoker<M extends HandlerMethodMetaInfo> extends DestroyableBase
        implements ActionHandlerInvoker, AfterInterceptorInvoker, ExceptionInterceptorInvoker {

    private static final Object[] DUMP_PARAMS = new Object[0];
    private App app;
    private ClassLoader cl;
    private ControllerClassMetaInfo controller;
    private Class<?> controllerClass;
    private MethodAccess methodAccess;
    private M handler;
    private int handlerIndex;
    private ConcurrentMap<H.Format, Boolean> templateCache = new ConcurrentHashMap<>();
    protected Method method; //
    private ParamValueLoaderService paramLoaderService;
    private JsonDTOClassManager jsonDTOClassManager;
    private final int paramCount;
    private final int fieldsAndParamsCount;
    private String singleJsonFieldName;
    private final boolean sessionFree;
    private final boolean express;
    private List<BeanSpec> paramSpecs;
    private Set<String> pathVariables;
    private CORS.Spec corsSpec;
    private CSRF.Spec csrfSpec;
    private boolean isStatic;
    private Object singleton;
    private H.Format forceResponseContentType;
    private H.Status forceResponseStatus;
    // Env doesn't match
    private boolean disabled;
    private String dspToken;
    private CacheSupportMetaInfo cacheSupport;
    // (field name: output name)
    private Map<Field, String> outputFields;
    // (param index: output name)
    private Map<Integer, String> outputParams;
    private boolean hasOutputVar;
    private String templateContext;
    private MissingAuthenticationHandler missingAuthenticationHandler;
    private MissingAuthenticationHandler csrfFailureHandler;

    private ReflectedHandlerInvoker(M handlerMetaInfo, App app) {
        this.app = app;
        this.cl = app.classLoader();
        this.handler = handlerMetaInfo;
        this.controller = handlerMetaInfo.classInfo();
        this.controllerClass = $.classForName(controller.className(), cl);
        this.disabled = !Env.matches(controllerClass);
        this.paramLoaderService = app.service(ParamValueLoaderManager.class).get(ActionContext.class);
        this.jsonDTOClassManager = app.service(JsonDTOClassManager.class);

        Class[] paramTypes = paramTypes(cl);
        try {
            method = controllerClass.getMethod(handlerMetaInfo.name(), paramTypes);
            this.disabled = this.disabled || !Env.matches(method);

        } catch (NoSuchMethodException e) {
            throw E.unexpected(e);
        }
        this.isStatic = handlerMetaInfo.isStatic();
        if (!this.isStatic) {
            //constructorAccess = ConstructorAccess.get(controllerClass);
            methodAccess = MethodAccess.get(controllerClass);
            handlerIndex = methodAccess.getIndex(handlerMetaInfo.name(), paramTypes);
        } else {
            method.setAccessible(true);
        }

        sessionFree = method.isAnnotationPresent(SessionFree.class);
        express = method.isAnnotationPresent(NonBlock.class);

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
        if (!isStatic) {
            this.singleton = ReflectedInvokerHelper.tryGetSingleton(controllerClass, app);
        }

        ResponseContentType contentType = getAnnotation(ResponseContentType.class);
        if (null != contentType) {
            forceResponseContentType = contentType.value().format();
        }

        ResponseStatus status = method.getAnnotation(ResponseStatus.class);
        if (null != status) {
            forceResponseStatus = H.Status.of(status.value());
        }

        PreventDoubleSubmission dsp = method.getAnnotation(PreventDoubleSubmission.class);
        if (null != dsp) {
            dspToken = dsp.value();
            if (PreventDoubleSubmission.DEFAULT.equals(dspToken)) {
                dspToken = app.config().dspToken();
            }
        }

        initOutputVariables();
        initCacheParams();
        checkTemplateContext();
        initMissingAuthenticationAndCsrfCheckHandler();
    }

    @Override
    protected void releaseResources() {
        app = null;
        cl = null;
        controller = null;
        controllerClass = null;
        method = null;
        methodAccess = null;
        handler.destroy();
        handler = null;
        cacheSupport = null;
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

    @Override
    public CacheSupportMetaInfo cacheSupport() {
        return cacheSupport;
    }

    @Override
    public MissingAuthenticationHandler missingAuthenticationHandler() {
        return missingAuthenticationHandler;
    }

    @Override
    public MissingAuthenticationHandler csrfFailureHandler() {
        return csrfFailureHandler;
    }

    public Result handle(ActionContext context) throws Exception {
        if (disabled) {
            return ActNotFound.get();
        }

        String urlContext = this.controller.contextPath();
        if (S.notBlank(urlContext)) {
            context.urlContext(urlContext);
        }

        context.attribute("reflected_handler", this);
        if (null != templateContext && context.state().isHandling()) {
            context.templateContext(templateContext);
        }
        preventDoubleSubmission(context);
        processForceResponse(context);
        ensureJsonDTOGenerated(context);
        Object controller = controllerInstance(context);


        /*
         * We will send back response immediately when param validation
         * failed in the following cases:
         * a) this is a data endpoint and accept JSON data
         * b) there is no template associated with the endpoint
         *   TODO: fix me - if method use arbitrary templates, then this check will fail
         */
        boolean failOnViolation = context.acceptJson() || checkTemplate(context);

        Object[] params = params(controller, context);

        if (failOnViolation && context.hasViolation()) {
            String msg = context.violationMessage(";");
            return new BadRequest(msg);
        }

        try {
            return invoke(handler, context, controller, params);
        } finally {

            if (hasOutputVar) {
                fillOutputVariables(controller, params, context);
            }

            if (null == context.hasTemplate()) {
                // template path has been reset by app logic
                templateCache.remove(context.accept());
            }
            // ensure template is loaded as
            // request handler might change the
            // template path
            checkTemplate(context);
        }
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

    @Override
    public boolean express() {
        return express;
    }

    public CORS.Spec corsSpec() {
        return corsSpec;
    }

    @Override
    public CSRF.Spec csrfSpec() {
        return csrfSpec;
    }

    private void ensureJsonDTOGenerated(ActionContext context) {
        if ((0 == fieldsAndParamsCount) || !context.jsonEncoded() || (null != context.attribute(CTX_ATTR_KEY))) {
            return;
        }
        Class<? extends JsonDTO> dtoClass = jsonDTOClassManager.get(controllerClass, method);
        if (null == dtoClass) {
            // there are neither fields nor params
            return;
        }
        try {
            JsonDTO dto = JSON.parseObject(patchedJsonBody(context), dtoClass);
            context.attribute(CTX_ATTR_KEY, dto);
        } catch (JSONException e) {
            if (e.getCause() != null) {
                App.LOGGER.warn(e.getCause(), "error parsing JSON data");
            } else {
                App.LOGGER.warn(e, "error parsing JSON data");
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

    private Class[] paramTypes(ClassLoader cl) {
        int sz = handler.paramCount();
        Class[] ca = new Class[sz];
        for (int i = 0; i < sz; ++i) {
            HandlerParamMetaInfo param = handler.param(i);
            ca[i] = $.classForName(param.type().getClassName(), cl);
        }
        return ca;
    }

    private void processForceResponse(ActionContext actionContext) {
        if (null != forceResponseContentType) {
            actionContext.accept(forceResponseContentType);
        }
        if (null != forceResponseStatus) {
            actionContext.forceResponseStatus(forceResponseStatus);
        }
    }

    private void preventDoubleSubmission(ActionContext context) {
        if (null == dspToken) {
            return;
        }
        H.Request req = context.req();
        if (req.method().safe()) {
            return;
        }
        String tokenValue = context.paramVal(dspToken);
        if (S.blank(tokenValue)) {
            return;
        }
        H.Session session = context.session();
        String cacheKey = S.concat("DSP-", dspToken);
        String cached = session.cached(cacheKey);
        if (S.eq(tokenValue, cached)) {
            throw Conflict.get();
        }
        session.cacheFor1Min(cacheKey, tokenValue);
    }

    private Object controllerInstance(ActionContext context) {
        if (isStatic) {
            return null;
        }
        if (null != singleton) {
            return singleton;
        }
        String controllerName = controllerClass.getName();
        Object inst = context.__controllerInstance(controllerName);
        if (null == inst) {
            inst = paramLoaderService.loadHostBean(controllerClass, context);
            context.__controllerInstance(controllerName, inst);
        }
        return inst;
    }

    private void initCacheParams() {
        CacheFor cacheFor = method.getAnnotation(CacheFor.class);
        cacheSupport = null == cacheFor ? CacheSupportMetaInfo.disabled() :  CacheSupportMetaInfo.enabled(
                new CacheKeyBuilder(cacheFor, S.concat(controllerClass.getName(), ".", method.getName())),
                cacheFor.value(),
                cacheFor.supportPost()
        );
    }

    private void checkTemplateContext() {
        TemplateContext templateContext = method.getAnnotation(TemplateContext.class);
        if (null != templateContext) {
            this.templateContext = templateContext.value();
        } else {
            templateContext = controllerClass.getAnnotation(TemplateContext.class);
            if (null != templateContext) {
                this.templateContext = templateContext.value();
            }
        }
    }

    private void fillOutputVariables(Object controller, Object[] params, ActionContext context) {
        if (!isStatic) {
            for (Map.Entry<Field, String> entry : outputFields.entrySet()) {
                Field field = entry.getKey();
                String outputName = entry.getValue();
                try {
                    Object val = field.get(controller);
                    context.renderArg(outputName, val);
                } catch (IllegalAccessException e) {
                    throw E.unexpected(e);
                }
            }
            context.fieldOutputVarCount(outputFields.size());
        }
        if (0 == params.length) {
            return;
        }
        for (Map.Entry<Integer, String> entry : outputParams.entrySet()) {
            int i = entry.getKey();
            String outputName = entry.getValue();
            context.renderArg(outputName, params[i]);
        }
    }

    private void initMissingAuthenticationAndCsrfCheckHandler() {
        HandleMissingAuthentication hma = getAnnotation(HandleMissingAuthentication.class);
        if (null != hma) {
            missingAuthenticationHandler = hma.value().handler(hma.custom());
        }

        HandleCsrfFailure hcf = getAnnotation(HandleCsrfFailure.class);
        if (null != hcf) {
            csrfFailureHandler = hcf.value().handler(hcf.custom());
        }
    }

    private void initOutputVariables() {
        Set<String> outputNames = new HashSet<>();
        outputFields = new HashMap<>();
        if (!isStatic) {
            List<Field> fields = $.fieldsOf(controllerClass);
            for (Field field : fields) {
                Output output = field.getAnnotation(Output.class);
                if (null != output) {
                    String fieldName = field.getName();
                    String outputName = output.value();
                    if (S.blank(outputName)) {
                        outputName = fieldName;
                    }
                    E.unexpectedIf(outputNames.contains(outputName), "output name already used: %s", outputName);
                    field.setAccessible(true);
                    outputFields.put(field, outputName);
                    outputNames.add(outputName);
                }
            }
        }

        try {
            outputParams = new HashMap<>();
            Class<?>[] paramTypes = method.getParameterTypes();
            int len = paramTypes.length;
            if (0 == len) {
                return;
            }
            Annotation outputRequestParams = method.getAnnotation(OutputRequestParams.class);
            if (null != outputRequestParams) {
                DependencyInjector injector = app.injector();
                for (int i = 0; i < len; ++i) {
                    if (!injector.isProvided(paramTypes[i])) {
                        String outputName = handler.param(i).name();
                        outputParams.put(i, outputName);
                        outputNames.add(outputName);
                    }
                }
            } else {
                Annotation[][] aaa = method.getParameterAnnotations();
                for (int i = 0; i < len; ++i) {
                    Annotation[] aa = aaa[i];
                    if (null == aa) {
                        continue;
                    }
                    Output output = null;
                    for (int j = aa.length - 1; j >= 0; --j) {
                        Annotation a = aa[j];
                        if (a.annotationType() == Output.class) {
                            output = $.cast(a);
                            break;
                        }
                    }
                    if (null == output) {
                        continue;
                    }
                    String outputName = output.value();
                    if (S.blank(outputName)) {
                        HandlerParamMetaInfo paramMetaInfo = handler.param(i);
                        outputName = paramMetaInfo.name();
                    }
                    E.unexpectedIf(outputNames.contains(outputName), "output name already used: %s", outputName);
                    outputParams.put(i, outputName);
                    outputNames.add(outputName);
                }
            }
        } finally {
            hasOutputVar = !outputNames.isEmpty();
        }
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
            return notFoundOnMethod(null);
        }
        boolean hasTemplate = checkTemplate(context);
        if (hasTemplate && result instanceof RenderAny) {
            result = RenderTemplate.INSTANCE;
        }
        return Controller.Util.inferResult(handlerMetaInfo, result, context, hasTemplate);
    }

    public NotFound notFoundOnMethod(String message) {
        return ActNotFound.create(method, message);
    }

    public boolean checkTemplate(ActionContext context) {
        if (!context.state().isHandling()) {
            //we don't check template on interceptors
            return false;
        }
        Boolean hasTemplate = context.hasTemplate();
        if (null != hasTemplate) {
            return hasTemplate;
        }
        H.Format fmt = context.accept();
        hasTemplate = templateCache.get(fmt);
        if (null == hasTemplate || Act.isDev()) {
            hasTemplate = probeTemplate(fmt, context);
            templateCache.putIfAbsent(fmt, hasTemplate);
        }
        context.setHasTemplate(hasTemplate);
        return hasTemplate;
    }

    private boolean probeTemplate(H.Format fmt, ActionContext context) {
        if (!TemplatePathResolver.isAcceptFormatSupported(fmt)) {
            return false;
        } else {
            Template t = Act.viewManager().load(context);
            return t != null;
        }
    }

    private Object[] params(Object controller, ActionContext context) {
        if (0 == paramCount) {
            return DUMP_PARAMS;
        }
        return paramLoaderService.loadMethodParams(controller, method, context);
    }


    private <T extends Annotation> T getAnnotation(Class<T> annoType) {
        T anno = method.getAnnotation(annoType);
        if (null == anno) {
            anno = controllerClass.getAnnotation(annoType);
        }
        return anno;
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
        public boolean express() {
            return invoker.express();
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
        public boolean express() {
            return invoker.express();
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
        public boolean express() {
            return invoker.express();
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
        public boolean express() {
            return invoker.express();
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

    private static class CacheKeyBuilder extends $.F1<ActionContext, String> {
        private String[] keys;
        private final String base;

        CacheKeyBuilder(CacheFor cacheFor, String actionPath) {
            this.base = base(actionPath);
            this.keys = cacheFor.keys();
        }

        private String base(String actionPath) {
            S.Buffer buffer = S.newBuffer();
            String[] sa = actionPath.split("\\.");
            for (String s : sa) {
                buffer.append(s.charAt(0));
            }
            buffer.append(actionPath.hashCode());
            return buffer.toString();
        }

        @Override
        public String apply(ActionContext context) throws NotAppliedException, Osgl.Break {
            TreeMap<String, String> keyValues = keyValues(context);
            S.Buffer buffer = S.newBuffer(base);
            for (Map.Entry<String, String> entry : keyValues.entrySet()) {
                buffer.append("-").append(entry.getKey()).append(":").append(entry.getValue());
            }
            buffer.append(context.userAgent().isMobile() ? "M" : "B");
            return buffer.toString();
        }

        private TreeMap<String, String> keyValues(ActionContext context) {
            TreeMap<String, String> map = new TreeMap<>();
            if (keys.length > 0) {
                for (String key : keys) {
                    map.put(key, paramVal(key, context));
                }
            } else {
                for (String key : context.paramKeys()) {
                    map.put(key, paramVal(key, context));
                }
            }
            return map;
        }

        private String paramVal(String key, ActionContext context) {
            String[] allValues = context.paramVals(key);
            if (0 == allValues.length) {
                return "";
            } else if (1 == allValues.length) {
                return allValues[0];
            } else {
                return  $.toString2(allValues);
            }
        }
    }

}
