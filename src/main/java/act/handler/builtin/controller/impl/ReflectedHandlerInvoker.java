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

import static act.app.ActionContext.State.HANDLING;
import static act.app.ActionContext.State.INTERCEPTING;
import static org.osgl.util.Generics.buildTypeParamImplLookup;

import act.Act;
import act.Trace;
import act.annotations.*;
import act.apidoc.SampleData;
import act.app.*;
import act.cli.ReportProgress;
import act.conf.AppConfig;
import act.controller.*;
import act.controller.annotation.*;
import act.controller.builtin.ThrottleFilter;
import act.controller.captcha.RequireCaptcha;
import act.controller.meta.*;
import act.data.annotation.DateFormatPattern;
import act.data.annotation.Pattern;
import act.db.RequireDataBind;
import act.handler.*;
import act.handler.builtin.controller.*;
import act.handler.event.ReflectedHandlerInvokerInit;
import act.handler.event.ReflectedHandlerInvokerInvoke;
import act.inject.DependencyInjector;
import act.inject.SessionVariable;
import act.inject.param.*;
import act.job.Job;
import act.job.JobManager;
import act.job.TrackableWorker;
import act.plugin.ControllerPlugin;
import act.security.*;
import act.sys.Env;
import act.util.*;
import act.util.Output;
import act.util.ProgressGauge;
import act.view.*;
import act.ws.WebSocketConnectionManager;
import com.alibaba.fastjson.*;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.osgl.$;
import org.osgl.Lang;
import org.osgl.exception.NotAppliedException;
import org.osgl.exception.ToBeImplemented;
import org.osgl.http.H;
import org.osgl.inject.BeanSpec;
import org.osgl.mvc.annotation.*;
import org.osgl.mvc.result.*;
import org.osgl.util.*;
import org.w3c.dom.Document;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import javax.enterprise.context.ApplicationScoped;
import javax.validation.ConstraintViolation;

/**
 * Implement handler using
 * https://github.com/EsotericSoftware/reflectasm
 */
@ApplicationScoped
public class ReflectedHandlerInvoker<M extends HandlerMethodMetaInfo> extends LogSupportedDestroyableBase
        implements ActionHandlerInvoker, AfterInterceptorInvoker, ExceptionInterceptorInvoker {

    private static final Object[] DUMP_PARAMS = new Object[0];
    private boolean mock;
    private App app;
    private AppConfig config;
    private ControllerClassMetaInfo controller;
    private Class<?> controllerClass;
    private MethodAccess methodAccess;
    private M handler;
    private int handlerIndex;
    private ConcurrentMap<H.Format, Boolean> templateAvailabilityCache = new ConcurrentHashMap<>();
    private $.Visitor<H.Format> templateChangeListener = new $.Visitor<H.Format>() {
        @Override
        public void visit(H.Format format) throws $.Break {
            templateAvailabilityCache.remove(format);
        }
    };
    protected Method method; //
    private ParamValueLoaderService paramLoaderService;
    private JsonDtoClassManager jsonDTOClassManager;
    private int paramCount;
    private int fieldsAndParamsCount;
    private int sessionVariablesCount;
    private String singleJsonFieldName;
    private boolean sessionFree;
    private boolean express;
    private boolean skipEvents;
    private List<BeanSpec> paramSpecs;
    private CORS.Spec corsSpec;
    private CSRF.Spec csrfSpec;
    private String csp;
    private boolean disableCsp;
    private boolean isStatic;
    private boolean requireCaptcha;
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
    private String dateFormatPattern;
    private boolean noTemplateCache;
    private MissingAuthenticationHandler missingAuthenticationHandler;
    private MissingAuthenticationHandler csrfFailureHandler;
    private ThrottleFilter throttleFilter;
    private boolean async;
    private boolean reportAsyncProgress;
    private boolean byPassImplicityTemplateVariable;
    private boolean forceDataBinding;
    private boolean isLargeResponse;
    private boolean forceSmallResponse;
    private Class<? extends SerializeFilter> filters[];
    private SerializerFeature features[];
    private PropertyNamingStrategy propertyNamingStrategy;
    private $.Function<ActionContext, Result> pluginBeforeHandler;
    private $.Func2<Result, ActionContext, Result> pluginAfterHandler;
    private Map<String, Object> attributes = new HashMap<>();
    private boolean enableCircularReferenceDetect;
    // it shall do full JSON string check when Accept is JSON and return type is String
    // however if the first full check is good, then the following check shall rely on quick check
    private boolean returnString;
    private boolean fullJsonStringChecked;
    private boolean fullJsonStringCheckFailure;
    // See https://github.com/actframework/actframework/issues/797
    private boolean suppressJsonDateFormat;
    // see https://github.com/actframework/actframework/issues/829
    private String downloadFilename;
    // see https://github.com/actframework/actframework/issues/835
    private ReturnValueAdvice returnValueAdvice;
    // see https://github.com/actframework/actframework/issues/852
    private boolean returnIterable;
    // see https://github.com/actframework/actframework/issues/922
    private ValidateViolationAdvice validateViolationAdvice;
    private boolean returnSimpleType;
    private boolean returnIterableComponentIsSimpleType;
    private boolean shallTransformReturnVal;
    private int order;
    private String xmlRootTag;
    private List<JsonDtoPatch> dtoPatches = new ArrayList<>();
    private boolean hasDtoPatches;
    private Class<?> returnType;
    // see https://github.com/actframework/actframework/issues/1269
    private boolean allowQrCodeRendering;

    private ReflectedHandlerInvoker(M handlerMetaInfo, App app) {
        this.app = app;
        this.config = app.config();
        this.mock = config.mockServer();
        this.handler = handlerMetaInfo;
        this.controller = handlerMetaInfo.classInfo();
        this.controllerClass = app.classForName(controller.className());
        this.disabled = !Env.matches(controllerClass);
        this.paramLoaderService = app.service(ParamValueLoaderManager.class).get(ActionContext.class);
        this.jsonDTOClassManager = app.service(JsonDtoClassManager.class);
        this.xmlRootTag = config.xmlRootTag();

        Class[] paramTypes = paramTypes(app);
        method = $.getMethod(controllerClass, handlerMetaInfo.name(), paramTypes);
        E.unexpectedIf(null == method, "Unable to locate handler method: " + handlerMetaInfo.name());
        this.returnType = Generics.getReturnType(method, controllerClass);
        this.returnString = this.returnType == String.class;
        Integer priority = handler.priority();
        if (null != priority) {
            this.order = priority;
        } else {
            Order order = ReflectedInvokerHelper.getAnnotation(Order.class, method);
            this.order = null == order ? Order.HIGHEST_PRECEDENCE : order.value();
        }
        final boolean isBuiltIn = controllerClass.getName().startsWith("act.");
        if (handlerMetaInfo.hasReturn() && !isBuiltIn) {
            this.returnSimpleType = this.returnString || $.isSimpleType(returnType);
            this.returnIterable = !this.returnSimpleType && (handlerMetaInfo.isReturnArray() || Iterable.class.isAssignableFrom(returnType));
            if (this.returnIterable) {
                if (handlerMetaInfo.isReturnArray()) {
                    this.returnIterableComponentIsSimpleType = $.isSimpleType(method.getReturnType().getComponentType());
                } else {
                    Type type = method.getGenericReturnType();
                    if (type instanceof ParameterizedType) {
                        ParameterizedType ptype = $.cast(type);
                        Type[] typeParams = ptype.getActualTypeArguments();
                        if (typeParams.length > 1) {
                            this.returnIterableComponentIsSimpleType = false;
                        } else if (typeParams.length == 1) {
                            Type p0 = typeParams[0];
                            if (p0 instanceof Class) {
                                this.returnIterableComponentIsSimpleType = $.isSimpleType((Class) p0);
                            } else {
                                this.returnIterableComponentIsSimpleType = false;
                            }
                        }
                    } else {
                        warn("Cannot determine component type of handler method return type: " + method);
                        this.returnIterableComponentIsSimpleType = false;
                    }
                }
            }
            this.shallTransformReturnVal = handlerMetaInfo.hasReturn() && (returnIterable && !returnIterableComponentIsSimpleType) || (!returnSimpleType);
        }
        this.pluginBeforeHandler = ControllerPlugin.Manager.INST.beforeHandler(controllerClass, method);
        this.pluginAfterHandler = ControllerPlugin.Manager.INST.afterHandler(controllerClass, method);
        this.disabled = this.disabled || !Env.matches(method);
        this.forceDataBinding = ReflectedInvokerHelper.isAnnotationPresent(RequireDataBind.class, method);
        this.async = null != ReflectedInvokerHelper.getAnnotation(Async.class, method);
        this.reportAsyncProgress = null != ReflectedInvokerHelper.getAnnotation(ReportProgress.class, method);
        this.isStatic = handlerMetaInfo.isStatic();
        if (!this.isStatic) {
            methodAccess = MethodAccess.get(controllerClass);
            handlerIndex = methodAccess.getIndex(handlerMetaInfo.name(), paramTypes);
        } else {
            method.setAccessible(true);
        }
        this.allowQrCodeRendering = ReflectedInvokerHelper.isAnnotationPresent(AllowQrCodeRendering.class, method);

        if (!isBuiltIn && handlerMetaInfo.hasReturn() && null == ReflectedInvokerHelper.getAnnotation(NoReturnValueAdvice.class, method)) {
            ReturnValueAdvisor advisor = getAnnotation(ReturnValueAdvisor.class);
            if (null != advisor) {
                returnValueAdvice = app.getInstance(advisor.value());
            } else if (null == getAnnotation(NoReturnValueAdvice.class)) {
                returnValueAdvice = app.config().globalReturnValueAdvice();
            }
        }

        ValidateViolationAdvisor vAdvisor = getAnnotation(ValidateViolationAdvisor.class);
        if (null != vAdvisor) {
            validateViolationAdvice = app.getInstance(vAdvisor.value());
        } else if (null == getAnnotation(NoValidateViolationAdvice.class)) {
            validateViolationAdvice = app.config().globalValidateViolationAdvice();
        }

        if (method.isAnnotationPresent(RequireCaptcha.class)) {
            this.requireCaptcha = true;
        }

        this.suppressJsonDateFormat = shouldSuppressJsonDateFormat();

        Throttled throttleControl = ReflectedInvokerHelper.getAnnotation(Throttled.class, method);
        if (null != throttleControl) {
            int throttle = throttleControl.value();
            if (throttle < 1) {
                throttle = app.config().requestThrottle();
            }
            Throttled.ExpireScale expireScale = throttleControl.expireScale();
            throttleFilter = new ThrottleFilter(throttle, expireScale.enabled());
        }

        this.isLargeResponse = ReflectedInvokerHelper.getAnnotation(LargeResponse.class, method) != null;
        this.forceSmallResponse = ReflectedInvokerHelper.getAnnotation(SmallResponse.class, method) != null;
        if (isLargeResponse && forceSmallResponse) {
            warn("found both @LargeResponse and @SmallResponse, will ignore @SmallResponse");
            forceSmallResponse = false;
        }

        FastJsonFilter filterAnno = ReflectedInvokerHelper.getAnnotation(FastJsonFilter.class, method);
        if (null != filterAnno) {
            filters = filterAnno.value();
        }
        FastJsonFeature featureAnno = ReflectedInvokerHelper.getAnnotation(FastJsonFeature.class, method);
        if (null != featureAnno) {
            features = featureAnno.value();
        }
        FastJsonPropertyNamingStrategy propertyNamingStrategyAnno = ReflectedInvokerHelper.getAnnotation(FastJsonPropertyNamingStrategy.class, method);
        if (null != propertyNamingStrategyAnno) {
            propertyNamingStrategy = propertyNamingStrategyAnno.value();
        }
        enableCircularReferenceDetect = hasAnnotation(EnableCircularReferenceDetect.class);

        DateFormatPattern pattern = ReflectedInvokerHelper.getAnnotation(DateFormatPattern.class, method);
        if (null != pattern) {
            this.dateFormatPattern = pattern.value();
        } else {
            Pattern patternLegacy = ReflectedInvokerHelper.getAnnotation(Pattern.class, method);
            if (null != patternLegacy) {
                this.dateFormatPattern = patternLegacy.value();
            }
        }
        if (null != this.dateFormatPattern) {
            handlerMetaInfo.dateFormatPattern(this.dateFormatPattern);
        }

        if (controllerClass.isAnnotationPresent(ExpressController.class)) {
            sessionFree = true;
            express = true;
            skipEvents = true;
        } else {
            sessionFree = hasAnnotation(SessionFree.class);
            express = hasAnnotation(NonBlock.class);
            skipEvents = hasAnnotation(SkipBuiltInEvents.class);
        }
        noTemplateCache = method.isAnnotationPresent(Template.NoCache.class);

        paramCount = handler.paramCount();
        paramSpecs = jsonDTOClassManager.beanSpecs(controllerClass, method);
        for (BeanSpec spec : paramSpecs) {
            JsonDtoPatch patch = JsonDtoPatch.of(spec);
            if (null != patch) {
                dtoPatches.add(patch);
            }
        }
        hasDtoPatches = !dtoPatches.isEmpty();
        List<BeanSpec> paramSpecWithoutSessionVariables = new ArrayList<>();
        fieldsAndParamsCount = paramSpecs.size();
        for (BeanSpec spec : paramSpecs) {
            if (spec.hasAnnotation(SessionVariable.class)) {
                sessionVariablesCount++;
            } else {
                paramSpecWithoutSessionVariables.add(spec);
            }
        }
        if (1 == (fieldsAndParamsCount - sessionVariablesCount)) {
            singleJsonFieldName = paramSpecWithoutSessionVariables.get(0).name();
        }

        CORS.Spec corsSpec = CORS.spec(method).chain(CORS.spec(controllerClass));
        this.corsSpec = corsSpec;

        CSRF.Spec csrfSpec = CSRF.spec(method).chain(CSRF.spec(controllerClass));
        this.csrfSpec = csrfSpec;

        CSP.Disable cspDisableAnno = getAnnotation(CSP.Disable.class);
        if (null != cspDisableAnno) {
            this.disableCsp = true;
        }

        if (!this.disableCsp) {
            CSP cspAnno = getAnnotation(CSP.class);
            if (null != cspAnno) {
                this.csp = cspAnno.value();
            }
        }

        if (!isStatic) {
            this.singleton = ReflectedInvokerHelper.tryGetSingleton(controllerClass, app);
        }

        if (null != controllerClass.getAnnotation(JsonView.class)) {
            forceResponseContentType = H.MediaType.JSON.format();
        }
        if (null != controllerClass.getAnnotation(CsvView.class)) {
            forceResponseContentType = H.MediaType.CSV.format();
        }
        // ResponseContentType takes priority of JsonView
        ResponseContentType contentType = controllerClass.getAnnotation(ResponseContentType.class);
        if (null != contentType) {
            forceResponseContentType = contentType.value().format();
        }

        DownloadFilename downloadFilename = ReflectedInvokerHelper.getAnnotation(DownloadFilename.class, method);
        if (null != downloadFilename) {
            this.downloadFilename = downloadFilename.value();
        }

        // method annotation takes priority of class annotation
        if (null != ReflectedInvokerHelper.getAnnotation(JsonView.class, method)) {
            forceResponseContentType = H.MediaType.JSON.format();
        }
        if (null != ReflectedInvokerHelper.getAnnotation(CsvView.class, method)) {
            forceResponseContentType = H.MediaType.CSV.format();
        }
        contentType = ReflectedInvokerHelper.getAnnotation(ResponseContentType.class, method);
        if (null != contentType) {
            forceResponseContentType = contentType.value().format();
        }

        ResponseStatus status = ReflectedInvokerHelper.getAnnotation(ResponseStatus.class, method);
        if (null != status) {
            forceResponseStatus = H.Status.of(status.value());
        }

        PreventDoubleSubmission dsp = ReflectedInvokerHelper.getAnnotation(PreventDoubleSubmission.class, method);
        if (null != dsp) {
            dspToken = dsp.value();
            if (PreventDoubleSubmission.DEFAULT.equals(dspToken)) {
                dspToken = app.config().dspToken();
            }
        }

        byPassImplicityTemplateVariable = (controllerClass.isAnnotationPresent(NoImplicitTemplateVariable.class) ||
                ReflectedInvokerHelper.isAnnotationPresent(NoImplicitTemplateVariable.class, method));

        initOutputVariables();
        initCacheParams(app.config());
        initMissingAuthenticationAndCsrfCheckHandler();
        app.eventBus().emit(new ReflectedHandlerInvokerInit(this));
    }

    @Override
    public int hashCode() {
        return $.hc(method);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ReflectedHandlerInvoker) {
            return $.eq(((ReflectedHandlerInvoker) obj).method, method);
        }
        return false;
    }

    @Override
    public String toString() {
        return method.toString();
    }

    @Override
    protected void releaseResources() {
        app = null;
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
    public int order() {
        return this.order;
    }

    public M handlerMetaInfo() {
        return handler;

    }

    @Override
    public Integer priority() {
        return handler.priority();
    }

    @Override
    public Method invokeMethod() {
        return method;
    }

    public interface ReflectedHandlerInvokerVisitor extends Visitor, $.Func2<Class<?>, Method, Void> {
    }

    public ReflectedHandlerInvoker attribute(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    public <T> T attribute(String key) {
        return $.cast(attributes.get(key));
    }

    @Override
    public void accept(Visitor visitor) {
        ReflectedHandlerInvokerVisitor rv = (ReflectedHandlerInvokerVisitor) visitor;
        rv.apply(controllerClass, method);
    }

    public Class<?> controllerClass() {
        return controllerClass;
    }

    public Method method() {
        return method;
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

    public Result handle(final ActionContext context) {
        if (disabled) {
            if (INTERCEPTING == context.state()) {
                return null;
            }
            return ActNotFound.get();
        }

        if (null != throttleFilter) {
            Result throttleResult = throttleFilter.handle(context);
            if (null != throttleResult) {
                return throttleResult;
            }
        }

        Result result = pluginBeforeHandler.apply(context);
        if (null != result) {
            return result;
        }

        if (suppressJsonDateFormat) {
            context.suppressJsonDateFormat();
        }

        if (requireCaptcha) {
            context.markAsRequireCaptcha();
        }

        if (enableCircularReferenceDetect) {
            context.enableCircularReferenceDetect();
        }

        if (null != downloadFilename) {
            context.downloadFileName(downloadFilename);
        }

        if (allowQrCodeRendering) {
            context.markAllowQrCodeRendering();
        }

        context.setReflectedHandlerInvoker(this);
        if (!context.skipEvents()) {
            app.eventBus().emit(new ReflectedHandlerInvokerInvoke(this, context));
        }

        if (1 == fieldsAndParamsCount(context)) {
            context.allowIgnoreParamNamespace();
        } else {
            context.disallowIgnoreParamNamespace();
        }

        if (isLargeResponse) {
            context.setLargeResponse();
        }

        if (null != filters) {
            context.fastjsonFilters(filters);
        }

        if (null != features) {
            context.fastjsonFeatures(features);
        }

        if (null != propertyNamingStrategy) {
            context.fastjsonPropertyNamingStrategy(propertyNamingStrategy);
        }

        if (null != dateFormatPattern) {
            context.dateFormatPattern(dateFormatPattern);
        }

        if (byPassImplicityTemplateVariable && context.state().isHandling()) {
            context.byPassImplicitVariable();
        }

        context.templateChangeListener(templateChangeListener);
        if (noTemplateCache) {
            context.disableTemplateCaching();
        }

        context.currentMethod(method);

        String urlContext = this.controller.urlContext();
        if (S.notBlank(urlContext)) {
            context.urlContext(urlContext);
        }

        String templateContext = this.controller.templateContext();
        if (null != templateContext) {
            context.templateContext(templateContext);
        }
        preventDoubleSubmission(context);
        processForceResponse(context);
        if (forceDataBinding || context.state().isHandling()) {
            ensureJsonDtoGenerated(context);
        }
        final Object controller = controllerInstance(context);

        final Object[] params = params(controller, context);

        context.ensureCaptcha();

        Map<String, ConstraintViolation> violations = context.violations();
        if (HANDLING == context.state() && !violations.isEmpty()) {
            if (null != validateViolationAdvice) {
                Result r = null;
                try {
                    Object retVal = validateViolationAdvice.onValidateViolation(violations, context);
                    if (null != retVal) {
                        if (retVal instanceof Result) {
                            r = (Result) retVal;
                        } else {
                            String payload;
                            H.Format accept = context.accept();
                            boolean requireXML = accept.isSameTypeWith(H.Format.XML);
                            JSONObject json = $.deepCopy(retVal).to(JSONObject.class);
                            if (requireXML) {
                                Document doc = $.convert(json).to(Document.class);
                                payload = XML.toString(doc);
                                r = new RenderXML(payload);
                            } else {
                                payload = JSON.toJSONString(json);
                                context.resp().contentType(H.Format.JSON);
                                r = new RenderJSON(payload);
                            }
                            r.status(H.Status.BAD_REQUEST);
                        }
                    }
                } catch (Result e) {
                    r = e;
                }
                if (null != r) {
                    return r;
                }
            }

            /*
             * We will send back response immediately when param validation
             * failed in either of the following cases:
             * 1) this is an ajax call
             * 2) the accept content type is **NOT** html
             */
            boolean failOnViolation = context.isAjax() || !context.accept().isSameTypeWith(H.Format.HTML);

            if (failOnViolation) {
                String msg = context.violationMessage(";");
                return ActBadRequest.create(msg);
            }
        }

        if (async) {
            final JobManager jobManager = context.app().jobManager();
            final String jobId = jobManager.randomJobId();
            final String reqInfo = context.req().toString();
            context.attachmentName(); // ensure attachment name get initialized
            context.keep();
            jobManager.prepare(jobId, new TrackableWorker() {
                @Override
                protected void run(ProgressGauge progressGauge) {
                    if (isTraceEnabled()) {
                        trace("about invoking request handler[%s] asynchronously: %s", reqInfo, jobId);
                    }
                    try {
                        Object o = invoke(context, controller, params);
                        if (null == o) {
                            o = "done";
                        }
                        Map<String, Object> payload = C.Map("attachmentName", context.rawAttachmentName());
                        jobManager.cacheResult(jobId, o, method, payload);
                        context.progress().commitFinalState();
                        boolean canDestroy = false;
                        synchronized (context) {
                            if (context.isKeep()) {
                                context.unKeep();
                            } else {
                                canDestroy = true;
                            }
                        }
                        if (canDestroy) {
                            context.destroy();
                        }
                    } catch (Exception e) {
                        String errMsg = S.buffer("Error handling request: ").append(reqInfo).toString();
                        warn(e, errMsg);
                        ControllerProgressGauge gauge = context.progress();
                        gauge.fail(errMsg);
                        jobManager.cacheResult(jobId, C.Map("error", e.getLocalizedMessage()), method, null);
                        gauge.commitFinalState();
                    }
                }
            });
            context.setJobId(jobId);
            WebSocketConnectionManager wscm = app.getInstance(WebSocketConnectionManager.class);
            wscm.subscribe(context.session(), SimpleProgressGauge.wsJobProgressTag(jobId));
            jobManager.now(jobId);
            /*
             * We need to decide how to render the response here:
             * 1. if it require json, then return json response directly
             * 2. otherwise we want to check the {@link H.Request#rawAccept()},
             * 2.1 in case raw accept is HTML then cache current accept and render Async status page, once done render
             *     result page using cached accept
             * 2.2 in case raw accept is not HTML then cache current accept and render JSON response
             */
            H.Format wantedAccept = null;
            boolean renderJson = !reportAsyncProgress;
            if (!renderJson) {
                H.Request<?> req = context.req();
                wantedAccept = req.accept();
                renderJson = wantedAccept.isSameTypeWith(H.Format.JSON) || !req.rawAccept().isSameTypeWith(H.Format.HTML);
            }
            if (renderJson) {
                String resultUrl = context.router().fullUrl("/~/jobs/%s/result", jobId);
                return new RenderJSON(C.Map("jobId", jobId, "resultUrl", resultUrl));
            }
            context.renderArg("targetAccept", wantedAccept.name());
            context.accept(H.Format.HTML);
            context.templatePath("/act/asyncJob.html");
            context.renderArg("jobId", jobId);
            return RenderTemplate.get();
        }

        try {
            return pluginAfterHandler.apply(invoke(handler, context, controller, params), context);
        } finally {
            if (hasOutputVar) {
                fillOutputVariables(controller, params, context);
            }
        }
    }

    @Override
    public Result handle(Result result, ActionContext actionContext) {
        actionContext.setResult(result);
        return handle(actionContext);
    }

    @Override
    public Result handle(Exception e, ActionContext actionContext) {
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

    @Override
    public boolean skipEvents() {
        return skipEvents;
    }

    public CORS.Spec corsSpec() {
        return corsSpec;
    }

    @Override
    public CSRF.Spec csrfSpec() {
        return csrfSpec;
    }

    @Override
    public String contentSecurityPolicy() {
        return disableCsp ? null : csp;
    }

    public boolean disableContentSecurityPolicy() {
        return disableCsp;
    }

    public void setLargeResponseHint() {
        if (!this.forceSmallResponse) {
            this.isLargeResponse = true;
        }
    }

    public String singleJsonFieldName() {
        return singleJsonFieldName;
    }

    public int fieldsAndParamsCount() {
        return fieldsAndParamsCount;
    }

    private void cacheJsonDto(ActContext<?> context, JsonDto dto) {
        context.attribute(JsonDto.CTX_ATTR_KEY, dto);
    }

    private void ensureJsonDtoGenerated(ActionContext context) {
        if (0 == fieldsAndParamsCount || (!context.jsonEncoded() && !context.xmlEncoded())) {
            return;
        }
        Class<? extends JsonDto> dtoClass = jsonDTOClassManager.get(paramSpecs, controllerClass);
        if (null == dtoClass) {
            // there are neither fields nor params
            return;
        }
        try {
            String patchedJsonBody = patchedJsonBody(context);
            context.patchedJsonBody(patchedJsonBody);
            JsonDto dto = JSON.parseObject(patchedJsonBody, dtoClass);
            if (null != dto) {
                patchDtoBeans(dto);
                cacheJsonDto(context, dto);
            }
        } catch (JSONException e) {
            if (e.getCause() != null) {
                warn(e.getCause(), "error parsing JSON data");
            } else {
                warn(e, "error parsing JSON data");
            }
            throw new BadRequest(e.getCause());
        }
    }

    private void patchDtoBeans(JsonDto dto) {
        if (hasDtoPatches) {
            for (JsonDtoPatch patch : dtoPatches) {
                Object bean = dto.get(patch.name());
                if (null != bean) {
                    patch.applyChildren(bean);
                }
            }
        }
    }

    private int fieldsAndParamsCount(ActionContext context) {
        if (fieldsAndParamsCount < 2) {
            return fieldsAndParamsCount;
        }
        return fieldsAndParamsCount - context.pathVarCount() - sessionVariablesCount;
    }

    private String singleJsonFieldName(ActionContext context) {
        if (null != singleJsonFieldName) {
            return singleJsonFieldName;
        }
        for (BeanSpec spec : paramSpecs) {
            String name = spec.name();
            if (context.isPathVar(name)) {
                continue;
            }
            return name;
        }
        return null;
    }

    /**
     * Suppose method signature is: `public void foo(Foo foo)`, and a JSON content is
     * not `{"foo": {foo-content}}`, then wrap it as `{"foo": body}`
     */
    private String patchedJsonBody(ActionContext context) {
        String body = context.body();
        if (!config.allowJsonBodyPatch() || S.blank(body)) {
            return body;
        }
        if (context.xmlEncoded()) {
            Document doc = XML.read(body);
            JSONObject json = $.convert(doc).to(JSONObject.class);
            body = JSON.toJSONString(json.containsKey(xmlRootTag) ? json.get(xmlRootTag) : json);
        }
        if (1 < fieldsAndParamsCount(context)) {
            return body;
        }
        String theName = singleJsonFieldName(context);
        if (null == theName) {
            return body;
        }
        int theNameLen = theName.length();
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
                    int id = i - nameStart - 1;
                    if (c == '"') {
                        if (id < theNameLen) {
                            needPatch = true;
                        }
                        break;
                    }
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

    private Class[] paramTypes(App app) {
        int sz = handler.paramCount();
        Class[] ca = new Class[sz];
        for (int i = 0; i < sz; ++i) {
            HandlerParamMetaInfo param = handler.param(i);
            ca[i] = app.classForName(param.type().getClassName());
        }
        return ca;
    }

    private void processForceResponse(ActionContext actionContext) {
        if (null != forceResponseContentType && $.not(actionContext.paramVal("_accept"))) {
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
        Class host = controllerClass;
        if (host.isAssignableFrom(context.handlerClass())) {
            host = context.handlerClass();
        }
        Object inst = context.__controllerInstance(host);
        if (null == inst) {
            inst = paramLoaderService.loadHostBean(host, context);
            context.__controllerInstance(host, inst);
        }
        return inst;
    }

    private void initCacheParams(AppConfig config) {
        if (Act.isDev() && !config.cacheForOnDevMode()) {
            cacheSupport = CacheSupportMetaInfo.disabled();
            return;
        }
        CacheFor cacheFor = ReflectedInvokerHelper.getAnnotation(CacheFor.class, method);
        cacheSupport = null == cacheFor ? CacheSupportMetaInfo.disabled() : CacheSupportMetaInfo.enabled(
                new CacheKeyBuilder(cacheFor, S.concat(controllerClass.getName(), ".", method.getName())),
                cacheFor.id(),
                cacheFor.value(),
                cacheFor.supportPost(),
                cacheFor.usePrivate(),
                cacheFor.noCacheControl(),
                cacheFor.eTagOnly(),
                cacheFor.noCache()
        );
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
            Annotation outputRequestParams = ReflectedInvokerHelper.getAnnotation(OutputRequestParams.class, method);
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

    private Object mockReturn() {
        if (returnType == Void.class || returnType == void.class) {
            return null;
        }
        BeanSpec returnSpec = BeanSpec.of(method.getGenericReturnType(), Act.injector());
        return SampleData.generate(returnSpec, null);
    }

    private Result invoke(M handlerMetaInfo, ActionContext context, Object controller, Object[] params) {
        Object retVal;
        String invocationInfo = null;
        if (config.traceHandler()) {
            invocationInfo = S.fmt("%s(%s)", handlerMetaInfo.fullName(), $.toString2(params));
            Trace.LOGGER_HANDLER.trace(invocationInfo);
        }
        try {
            retVal = invoke(context, controller, params);
        } catch (Result r) {
            retVal = r;
        } catch (Exception e) {
            if (config.traceHandler()) {
                Trace.LOGGER_HANDLER.trace(e, "error invoking %s", invocationInfo);
            }
            throw e;
        }
        return transform(retVal, this, context, returnValueAdvice, shallTransformReturnVal, returnIterable);
    }

    private Object invoke(ActionContext context, Object controller, Object[] params) {
        Object retVal;
        try {
            retVal = null == methodAccess ? $.invokeStatic(method, params) : methodAccess.invoke(controller, handlerIndex, params);
        } catch (ToBeImplemented e) {
            if (mock) {
                retVal = mockReturn();
            } else {
                throw e;
            }
        }
        if (returnString && context.acceptJson()) {
            retVal = null == retVal ? null : ensureValidJson(S.string(retVal));
        }
        if (null != retVal) {
            context.calcResultHashForEtag(retVal);
        }
        return retVal;
    }

    private static Result transform(Object retVal, ReflectedHandlerInvoker invoker, ActionContext context, ReturnValueAdvice returnValueAdvice, boolean transformRetVal, boolean returnIterable) {
        if (context.resp().isClosed()) {
            return null;
        }
        if (retVal instanceof Result) {
            Result result = (Result) retVal;
            if (result.status().isError()) {
                throw result;
            }
            invoker.checkTemplate(context);
            return result;
        }
        final HandlerMethodMetaInfo handlerMetaInfo = invoker.handler;
        final boolean hasReturn = handlerMetaInfo.hasReturn() && !handlerMetaInfo.returnTypeInfo().isResult();
        if (null == retVal && hasReturn) {
            // ActFramework respond 404 Not Found when
            // handler invoker return `null`
            // and there are return type of the action method signature
            // and the return type is **NOT** Result
            return ActNotFound.create();
        }
        boolean hasTemplate = invoker.checkTemplate(context);
        if (!hasTemplate && hasReturn && null != returnValueAdvice) {
            if (transformRetVal) {
                PropertySpec.MetaInfo propertySpec = PropertySpec.MetaInfo.withCurrent(handlerMetaInfo, context);
                if (null != propertySpec) {
                    Lang._MappingStage stage = propertySpec.applyTo(Lang.map(retVal), context);
                    Object newRetVal = returnIterable ? new JSONArray() : new JSONObject();
                    retVal = stage.to(newRetVal);
                } else {
                    String curSpec = PropertySpec.current.get();
                    if (S.notBlank(curSpec)) {
                        Object newRetVal = returnIterable ? new JSONArray() : new JSONObject();
                        retVal = Lang.deepCopy(retVal).filter(curSpec).to(newRetVal);
                        PropertySpec.current.remove();
                    }
                }
            }
            retVal = returnValueAdvice.applyTo(retVal, context);
        }
        return Controller.Util.inferResult(handlerMetaInfo, retVal, context, hasTemplate);
    }

    private String ensureValidJson(String result) {
        if (S.blank(result)) {
            return "{}";
        }
        boolean looksOkay = (result.startsWith("{") && result.endsWith("}")) || (result.startsWith("[") && result.endsWith("]"));
        if (!looksOkay) {
            fullJsonStringCheckFailure = true;
            return "{\"result\":\"" + result + "\"}";
        }
        if (fullJsonStringCheckFailure || !fullJsonStringChecked) {
            fullJsonStringChecked = true;
            try {
                JSON.parse(result);
            } catch (Exception e) {
                fullJsonStringCheckFailure = true;
                return "{\"result\":\"" + result + "\"}";
            }
        }
        return result;
    }

    private boolean checkTemplate(ActionContext context) {
        if (!context.state().isHandling()) {
            //we don't check template on interceptors
            return false;
        }
        H.Format fmt = context.accept();
        if (noTemplateCache || Act.isDev()) {
            return probeTemplate(fmt, context);
        }
        Boolean hasTemplate = context.hasTemplate();
        if (null != hasTemplate) {
            return hasTemplate;
        }
        hasTemplate = templateAvailabilityCache.get(fmt);
        if (null == hasTemplate) {
            hasTemplate = probeTemplate(fmt, context);
            templateAvailabilityCache.putIfAbsent(fmt, hasTemplate);
        }
        context.setHasTemplate(hasTemplate);
        return hasTemplate;
    }

    private boolean probeTemplate(H.Format fmt, ActionContext context) {
        if (!TemplatePathResolver.isAcceptFormatSupported(fmt)) {
            return false;
        } else {
            Template t = Act.viewManager().load(context);
            boolean hasTemplate = null != t;
            context.setHasTemplate(hasTemplate);
            return hasTemplate;
        }
    }

    private Object[] params(Object controller, ActionContext context) {
        if (0 == paramCount) {
            return DUMP_PARAMS;
        }
        return paramLoaderService.loadMethodParams(controller, method, context);
    }


    public <T extends Annotation> T getAnnotation(Class<T> annoType) {
        T anno = ReflectedInvokerHelper.getAnnotation(annoType, method);
        if (null == anno) {
            anno = controllerClass.getAnnotation(annoType);
        }
        return anno;
    }

    public boolean hasAnnotation(Class<? extends Annotation> annoType) {
        return (null != ReflectedInvokerHelper.getAnnotation(annoType, method)) || null != controllerClass.getAnnotation(annoType);
    }

    private boolean shouldSuppressJsonDateFormat() {
        List<Field> fields = $.fieldsOf(returnType);
        for (Field field : fields) {
            JSONField jsonField = field.getAnnotation(JSONField.class);
            if (null != jsonField) {
                if (S.notBlank(jsonField.format())) {
                    return true;
                }
            }
        }
        return false;
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
        public int order() {
            return invoker.order();
        }

        @Override
        public int hashCode() {
            return $.hc(invoker, getClass());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof _Before) {
                return $.eq(((_Before) obj).invoker, invoker);
            }
            return false;
        }

        @Override
        public String toString() {
            return invoker.toString();
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
        public boolean skipEvents() {
            return invoker.skipEvents();
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
        public int hashCode() {
            return $.hc(invoker, getClass());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof _After) {
                return $.eq(((_After) obj).invoker, invoker);
            }
            return false;
        }

        @Override
        public String toString() {
            return invoker.toString();
        }

        @Override
        public int order() {
            return invoker.order();
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
        public boolean skipEvents() {
            return invoker.skipEvents();
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
            App app = Act.app();
            for (String cn : classNames) {
                clsList.add((Class) app.classForName(cn));
            }
            return clsList;
        }

        @Override
        public int hashCode() {
            return $.hc(invoker, getClass());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof _Exception) {
                return $.eq(((_Exception) obj).invoker, invoker);
            }
            return false;
        }

        @Override
        public String toString() {
            return invoker.toString();
        }

        @Override
        public int order() {
            return invoker.order();
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
        public boolean skipEvents() {
            return invoker.skipEvents();
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
        public int hashCode() {
            return $.hc(invoker, getClass());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof _Finally) {
                return $.eq(((_Finally) obj).invoker, invoker);
            }
            return false;
        }

        @Override
        public String toString() {
            return invoker.toString();
        }

        @Override
        public int order() {
            return invoker.order();
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
        public boolean skipEvents() {
            return invoker.skipEvents();
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
        public String apply(ActionContext context) throws NotAppliedException, $.Break {
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
            map.put("__accept__", context.accept().name());
            return map;
        }

        private String paramVal(String key, ActionContext context) {
            String[] allValues = context.paramVals(key);
            if (0 == allValues.length) {
                return "";
            } else if (1 == allValues.length) {
                return allValues[0];
            } else {
                return $.toString2(allValues);
            }
        }
    }

}
