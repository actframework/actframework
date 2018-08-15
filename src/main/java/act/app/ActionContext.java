package act.app;

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

import static act.controller.Controller.Util.*;
import static org.osgl.http.H.Header.Names.*;

import act.*;
import act.conf.AppConfig;
import act.controller.ResponseCache;
import act.controller.captcha.CaptchaViolation;
import act.data.MapUtil;
import act.data.RequestBodyParser;
import act.event.ActEvent;
import act.event.SystemEvent;
import act.handler.RequestHandler;
import act.handler.builtin.controller.impl.ReflectedHandlerInvoker;
import act.i18n.LocaleResolver;
import act.route.Router;
import act.route.UrlPath;
import act.security.CORS;
import act.session.SessionManager;
import act.util.*;
import act.view.RenderAny;
import act.xio.undertow.UndertowRequest;
import org.osgl.$;
import org.osgl.concurrent.ContextLocal;
import org.osgl.http.H;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.mvc.result.Result;
import org.osgl.storage.ISObject;
import org.osgl.util.*;
import org.osgl.web.util.UserAgent;

import java.lang.annotation.Annotation;
import java.util.*;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;

/**
 * {@code AppContext} encapsulate contextual properties needed by
 * an application session
 */
@RequestScoped
public class ActionContext extends ActContext.Base<ActionContext> implements Destroyable {

    private static final Logger LOGGER = LogManager.get(ActionContext.class);

    public static final String ATTR_EXCEPTION = "__exception__";
    public static final String ATTR_CURRENT_FILE_INDEX = "__file_id__";
    public static final String REQ_BODY = "_body";

    private H.Request request;
    private ActResponse<?> response;
    private H.Session session;
    private H.Flash flash;
    private Set<Map.Entry<String, String[]>> requestParamCache;
    private Map<String, String> extraParams;
    private volatile Map<String, String[]> bodyParams;
    private Map<String, String[]> allParams;
    private String actionPath; // e.g. com.mycorp.myapp.controller.AbcController.foo
    private State state;
    private Map<String, Object> controllerInstances;
    private Map<String, ISObject[]> uploads;
    private Router router;
    private String processedUrl;
    private RequestHandler handler;
    private UserAgent ua;
    private String sessionKeyUsername;
    private boolean sessionPassThrough;
    private LocaleResolver localeResolver;
    private boolean disableCors;
    private boolean disableCsrf;
    private Boolean hasTemplate;
    private $.Visitor<H.Format> templateChangeListener;
    private H.Status forceResponseStatus;
    private boolean cacheEnabled;
    private MissingAuthenticationHandler forceMissingAuthenticationHandler;
    private MissingAuthenticationHandler forceCsrfCheckingFailureHandler;
    private String urlContext;
    private boolean byPassImplicitTemplateVariable;
    private boolean isLargeResponse;
    private boolean requireCaptcha;
    private int pathVarCount;
    private UrlPath urlPath;
    private Set<String> pathVarNames = new HashSet<>();
    private SessionManager sessionManager;
    private Trace.AccessLog accessLog;
    private ReflectedHandlerInvoker reflectedHandlerInvoker;
    private boolean requireBodyParsing;
    private boolean allowIgnoreParamNamespace;
    private boolean consumed;
    private boolean readyForDestroy;
    private int resultHash = Integer.MIN_VALUE;
    private PropertySpec.MetaInfo propSpec;

    // see https://github.com/actframework/actframework/issues/492
    public String encodedSessionToken;

    // -- replace attributres with fields -- perf tune
    // -- ATTR_CSRF_PREFETCHED
    private String csrfPrefetched;
    public void setCsrfPrefetched(String csrf) {
        csrfPrefetched = csrf;
    }
    public String csrfPrefetched() {
        return csrfPrefetched;
    }
    public void clearCsrfPrefetched() {
        csrfPrefetched = null;
    }
    // -- ATTR_WAS_UNAUTHENTICATED
    private boolean wasUnauthenticated;
    public boolean wasUnauthenticated() {
        return wasUnauthenticated;
    }
    public void setWasUnauthenticated() {
        wasUnauthenticated = true;
    }
    public void clearWasUnauthenticated() {
        wasUnauthenticated = false;
    }
    // -- ATTR_HANDLER
    // replaced with field handler
    // -- ATTR_RESULT
    private Result result;
    public Result result() {
        return result;
    }
    public void setResult(Result result) {
        this.result = result;
    }

    @Inject
    private ActionContext(App app, H.Request request, ActResponse<?> response) {
        super(app, true);
        E.NPE(app, request, response);
        request.context(this);
        response.context(this);
        this.request = request;
        this.response = response;
        this.accessLog = app.config().traceRequests() ? Trace.AccessLog.create(request) : null;
        this._init();
        this.state = State.CREATED;
        AppConfig config = app.config();
        this.disableCors = !config.corsEnabled();
        this.disableCsrf = req().method().safe();
        this.sessionKeyUsername = config.sessionKeyUsername();
        this.sessionPassThrough = config.sessionPassThrough();
        this.localeResolver = new LocaleResolver(this);
        this.sessionManager = app.sessionManager();
    }

    public State state() {
        return state;
    }

    public boolean isSessionDissolved() {
        return state == State.SESSION_DISSOLVED;
    }

    public boolean isSessionResolved() {
        return state == State.SESSION_RESOLVED;
    }

    public H.Request req() {
        return request;
    }

    public ActResponse<?> resp() {
        return response;
    }

    public ActResponse<?> prepareRespForResultEvaluation() {
        response.onResult();
        return response;
    }

    public boolean skipEvents() {
        if (null == handler) {
            return true;
        }
        return handler.skipEvents(this);
    }

    public void markAsConsumed() {
        consumed = true;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void markAsReadyForClose() {
        readyForDestroy = true;
    }

    public boolean isReadyForDestroy() {
        return readyForDestroy;
    }

    public H.Cookie cookie(String name) {
        return req().cookie(name);
    }

    public H.Session session() {
        return session;
    }

    public String session(String key) {
        return session.get(key);
    }

    public H.Session session(String key, String value) {
        return session.put(key, value);
    }

    /**
     * Returns HTTP session's id
     * @return HTTP session id
     */
    public String sessionId() {
        return session().id();
    }

    public H.Flash flash() {
        return flash;
    }

    public String flash(String key) {
        return flash.get(key);
    }

    public H.Flash flash(String key, String value) {
        return flash.put(key, value);
    }

    public Router router() {
        return router;
    }

    public ActionContext router(Router router) {
        this.router = $.requireNotNull(router);
        return this;
    }

    public ActionContext processedUrl(String url) {
        this.processedUrl = url;
        return this;
    }

    private String processedUrl() {
        return null == processedUrl ? req().url() : processedUrl;
    }

    public String attachmentName() {
        String s = processedUrl;
        if (s.contains("/")) {
            s = S.cut(s).afterLast("/");
        } else {
            s = methodPath();
            if (s.contains(".")) {
                s = S.cut(s).afterLast(".");
            }
        }
        return s + "." + accept().name();
    }

    public void markAsRequireCaptcha() {
        this.requireCaptcha = true;
    }

    public void ensureCaptcha() {
        if (app().captchaManager().disabled()) {
            return;
        }
        if (!subjectToCaptchaProtection()) {
            return;
        }
        if (verifyReCaptcha()) {
            return;
        }
        if (!verifyActCaptcha()) {
            addViolation("captcha", new CaptchaViolation());
        }
    }

    private boolean verifyReCaptcha() {
        return app().httpClientService().verifyReCaptchaResponse(this);
    }

    private boolean verifyActCaptcha() {
        String token = paramVal("a-captcha-token");
        if (S.blank(token)) {
            return false;
        }
        String answer = paramVal("a-captcha-answer");
        if (S.blank(answer)) {
            return false;
        }
        Token theToken = app().crypto().parseToken(token);
        if (!theToken.isValid()) {
            return false;
        }
        theToken.consume();
        return S.eq(answer, theToken.id());
    }

    private boolean subjectToCaptchaProtection() {
        if (!requireCaptcha) {
            return false;
        }
        H.Method method = req().method();
        return method == H.Method.POST || method == H.Method.PUT || method == H.Method.PATCH;
    }

    public ActionContext calcResultHashForEtag(Object o) {
        if (cacheEnabled && null != o && !(o instanceof Result)) {
            resultHash = $.hc(o);
        }
        return this;
    }

    public void applyResultHashToEtag() {
        if (resultHash > Integer.MIN_VALUE) {
            resp().addHeaderIfNotAdded(ETAG, S.string(resultHash));
        }
    }

    public ActionContext propertySpec(PropertySpec.MetaInfo spec) {
        this.propSpec = spec;
        return this;
    }

    public PropertySpec.MetaInfo propertySpec() {
        return this.propSpec;
    }

    public MissingAuthenticationHandler missingAuthenticationHandler() {
        if (null != forceMissingAuthenticationHandler) {
            return forceMissingAuthenticationHandler;
        }
        return isAjax() ? config().ajaxMissingAuthenticationHandler() : config().missingAuthenticationHandler();
    }

    public MissingAuthenticationHandler csrfFailureHandler() {
        if (null != forceCsrfCheckingFailureHandler) {
            return forceCsrfCheckingFailureHandler;
        }
        return isAjax() ? config().ajaxCsrfCheckFailureHandler() : config().csrfCheckFailureHandler();
    }

    public ActionContext forceMissingAuthenticationHandler(MissingAuthenticationHandler handler) {
        this.forceMissingAuthenticationHandler = handler;
        return this;
    }

    public ActionContext forceCsrfCheckingFailureHandler(MissingAuthenticationHandler handler) {
        this.forceCsrfCheckingFailureHandler = handler;
        return this;
    }

    public ActionContext byPassImplicitVariable() {
        this.byPassImplicitTemplateVariable = true;
        return this;
    }

    public boolean isByPassImplicitTemplateVariable() {
        return this.byPassImplicitTemplateVariable;
    }

    public ActionContext urlContext(String context) {
        this.urlContext = context;
        return this;
    }

    public String urlContext() {
        return urlContext;
    }

    /**
     * Return a {@link UrlPath} of this context.
     *
     * Note this method is used only by {@link Router} for dynamic path
     * matching.
     *
     * @return a {@link UrlPath} of this context
     */
    public UrlPath urlPath() {
        if (null == urlPath) {
            urlPath = UrlPath.of(req().path());
        }
        return urlPath;
    }

    public void forward(String url) {
        E.illegalArgumentIfNot(url.startsWith("/"), "forward URL must starts with single '/'");
        E.illegalArgumentIf(url.startsWith("//"), "forward URL must starts with single `/`");
        E.unexpectedIfNot(H.Method.GET == req().method(), "forward only support on HTTP GET request");
        uploads.clear();
        extraParams.clear();
        bodyParams = null;
        urlPath = UrlPath.of(url);
        UndertowRequest req = $.cast(req());
        state = State.CREATED;
        req.forward(url);
        final RequestHandler requestHandler = router.getInvoker(H.Method.GET, url, this);
        requestHandler.handle(this);
    }

    // !!!IMPORTANT! the following methods needs to be kept to allow enhancer work correctly
    @Override
    public <T> T renderArg(String name) {
        return super.renderArg(name);
    }

    @Override
    public ActionContext renderArg(String name, Object val) {
        return super.renderArg(name, val);
    }

    @Override
    public Map<String, Object> renderArgs() {
        return super.renderArgs();
    }

    @Override
    public ActionContext templatePath(String templatePath) {
        hasTemplate = null;
        if (null != templateChangeListener) {
            templateChangeListener.visit(accept());
        }
        return super.templatePath(templatePath);
    }

    @Override
    public ActionContext templateLiteral(String literal) {
        // Need to declare this method for bytecode enhancement
        return super.templateLiteral(literal);
    }

    public ActionContext templateChangeListener($.Visitor<H.Format> listener) {
        this.templateChangeListener = $.requireNotNull(listener);
        return this;
    }

    public RequestHandler handler() {
        return handler;
    }

    public ActionContext handler(RequestHandler handler) {
        this.handler = $.requireNotNull(handler);
        return this;
    }

    public H.Format accept() {
        return req().accept();
    }

    public ActionContext accept(H.Format fmt) {
        req().accept(fmt);
        return this;
    }

    public Boolean hasTemplate() {
        return hasTemplate;
    }

    public ActionContext setHasTemplate(boolean b) {
        hasTemplate = b;
        return this;
    }

    public ActionContext enableCache() {
        E.illegalArgumentIf(this.cacheEnabled, "cache already enabled in the action context");
        this.cacheEnabled = true;
        this.response = new ResponseCache(response);
        return this;
    }

    public void markRequireBodyParsing() {
        requireBodyParsing = true;
    }

    public boolean isAllowIgnoreParamNamespace() {
        return allowIgnoreParamNamespace;
    }

    public void allowIgnoreParamNamespace() {
        allowIgnoreParamNamespace = true;
    }

    public void disallowIgnoreParamNamespace() {
        allowIgnoreParamNamespace = false;
    }

    public int pathVarCount() {
        return pathVarCount;
    }

    public boolean isPathVar(String name) {
        return pathVarNames.contains(name);
    }

    public String portId() {
        return router().portId();
    }

    public int port() {return router().port(); }

    public UserAgent userAgent() {
        if (null == ua) {
            ua = UserAgent.parse(req().header(H.Header.Names.USER_AGENT));
        }
        return ua;
    }

    public boolean jsonEncoded() {
        return req().contentType() == H.Format.JSON;
    }

    public boolean acceptJson() {
        return accept() == H.Format.JSON;
    }

    public boolean acceptXML() {
        return accept() == H.Format.XML;
    }

    public boolean isAjax() {
        return req().isAjax();
    }

    public boolean isOptionsMethod() {
        return req().method() == H.Method.OPTIONS;
    }

    public void setLargeResponse() {
        isLargeResponse = true;
    }

    public void setReflectedHandlerInvoker(ReflectedHandlerInvoker invoker) {
        this.reflectedHandlerInvoker = invoker;
    }

    public void setLargeResponseHint() {
        if (null != reflectedHandlerInvoker) {
            reflectedHandlerInvoker.setLargeResponseHint();
        }
    }

    public boolean isLargeResponse() {
        return isLargeResponse;
    }

    public String username() {
        return session().get(sessionKeyUsername);
    }

    public boolean isLoggedIn() {
        return S.notBlank(username());
    }

    public String body() {
        return paramVal(REQ_BODY);
    }

    public ActionContext param(String name, String value) {
        extraParams.put(name, value);
        return this;
    }

    public ActionContext urlPathParam(String name, String value) {
        pathVarCount++;
        pathVarNames.add(name);
        return param(name, value);
    }

    @Override
    public Set<String> paramKeys() {
        Set<String> set = new HashSet<String>();
        set.addAll(C.<String>list(request.paramNames()));
        set.addAll(extraParams.keySet());
        set.addAll(bodyParams().keySet());
        return set;
    }

    @Override
    public String paramVal(String name) {
        String val = _paramVal(name);
        return null != val ? val : allowIgnoreParamNamespace && name.contains(".") ? _paramVal(S.cut(name).afterFirst(".")) : null;
    }

    private String _paramVal(String name) {
        String val = extraParams.get(name);
        if (null != val) {
            return val;
        }
        val = request.paramVal(name);
        if (null == val) {
            String[] sa = getBody(name);
            if (null != sa && sa.length > 0) {
                val = sa[0];
            }
        }
        return val;
    }

    public String paramValwithoutBodyParsing(String name) {
        String val = extraParams.get(name);
        if (null != val) {
            return val;
        }
        val = request.paramVal(name);
        if (null == val && null != bodyParams) {
            String[] sa = getBody(name);
            if (null != sa && sa.length > 0) {
                val = sa[0];
            }
        }
        return val;
    }

    public String[] paramVals(String name) {
        String val = extraParams.get(name);
        if (null != val) {
            return new String[]{val};
        }
        String[] sa = request.paramVals(name);
        return null == sa ? getBody(name) : sa;
    }

    private String[] getBody(String name) {
        Map<String, String[]> body = bodyParams();
        String[] sa = body.get(name);
        return null == sa ? new String[0] : sa;
    }

    private Map<String, String[]> bodyParams() {
        if (null == bodyParams) {
            Map<String, String[]> map = new HashMap<>();
            H.Method method = request.method();
            if (H.Method.POST == method || H.Method.PUT == method || H.Method.PATCH == method) {
                RequestBodyParser parser = RequestBodyParser.get(request);
                map = parser.parse(this);
            }
            bodyParams = map;
            router.markRequireBodyParsing(handler);
        }
        return bodyParams;
    }

    public Map<String, String[]> allParams() {
        return allParams;
    }

    public ISObject upload(String name) {
        Integer index = attribute(ATTR_CURRENT_FILE_INDEX);
        if (null == index) {
            index = 0;
        }
        return upload(name, index);
    }

    public ISObject upload(String name, int index) {
        body();
        ISObject[] a = uploads.get(name);
        return null != a && a.length > index ? a[index] : null;
    }

    public ActionContext addUpload(String name, ISObject sobj) {
        ISObject[] a = uploads.get(name);
        if (null == a) {
            a = new ISObject[1];
            a[0] = sobj;
        } else {
            ISObject[] newA = new ISObject[a.length + 1];
            System.arraycopy(a, 0, newA, 0, a.length);
            newA[a.length] = sobj;
            a = newA;
        }
        uploads.put(name, a);
        return this;
    }

    public H.Status successStatus() {
        if (null != forceResponseStatus) {
            return forceResponseStatus;
        }
        return H.Method.POST == req().method() ? H.Status.CREATED : H.Status.OK;
    }

    public ActionContext forceResponseStatus(H.Status status) {
        this.forceResponseStatus = $.requireNotNull(status);
        return this;
    }

    public Result nullValueResult() {
        if (hasRenderArgs()) {
            RenderAny result = new RenderAny();
            if (renderArgs().size() == fieldOutputVarCount() && req().isAjax()) {
                result.ignoreMissingTemplate();
            }
            return result;
        }
        return nullValueResultIgnoreRenderArgs();
    }

    public Result nullValueResultIgnoreRenderArgs() {
        if (null != forceResponseStatus) {
            return new Result(forceResponseStatus){};
        } else {
            if (req().method() == H.Method.POST) {
                H.Format accept = accept();
                if (H.Format.JSON == accept) {
                    return CREATED_JSON;
                } else if (H.Format.XML == accept) {
                    return CREATED_XML;
                } else {
                    return CREATED;
                }
            } else {
                return NO_CONTENT;
            }
        }
    }

    public void preCheckCsrf() {
        if (!disableCsrf) {
            handler().csrfSpec().preCheck(this);
        }
    }

    public void checkCsrf(H.Session session) {
        if (!disableCsrf) {
            handler().csrfSpec().check(this, session);
        }
    }

    public void setCsrfCookieAndRenderArgs() {
        handler().csrfSpec().setCookieAndRenderArgs(this);
    }

    public void disableCORS() {
        this.disableCors = true;
    }

    /**
     * Apply content type to response with result provided.
     *
     * If `result` is an error then it might not apply content type as requested:
     * * If request is not ajax request, then use `text/html`
     * * If request is ajax request then apply requested content type only when `json` or `xml` is requested
     * * otherwise use `text/html`
     *
     * @param result
     *      the result used to check if it is error result
     * @return
     *      this `ActionContext`.
     */
    public ActionContext applyContentType(Result result) {
        if (!result.status().isError()) {
            return applyContentType();
        }
        if (req().isAjax()) {
            H.Request req = req();
            H.Format fmt = req.accept();
            if (H.Format.UNKNOWN == fmt) {
                fmt = req.contentType();
            }
            if (H.Format.JSON == fmt || H.Format.XML == fmt) {
                applyContentType(fmt);
            } else {
                applyContentType(H.Format.HTML);
            }
        } else {
            applyContentType(H.Format.HTML);
        }
        return this;
    }

    public ActionContext applyContentType() {
        ActResponse resp = resp();
        H.Format lastContentType = resp.lastContentType();
        if (null != lastContentType && $.ne(H.Format.UNKNOWN, lastContentType)) {
            resp.commitContentType();
            return this;
        }
        H.Request req = req();
        H.Format fmt = req.accept();
        if (H.Format.UNKNOWN == fmt) {
            fmt = req.contentType();
        }
        applyContentType(fmt);
        return this;
    }

    public ActionContext applyCorsSpec() {
        RequestHandler handler = handler();
        if (null != handler) {
            CORS.Spec spec = handler.corsSpec();
            spec.applyTo(this);
        }
        applyGlobalCorsSetting();
        return this;
    }

    public ActionContext applyContentSecurityPolicy() {
        RequestHandler handler = handler();
        if (null != handler) {
            boolean disableCSP = handler.disableContentSecurityPolicy();
            if (disableCSP) {
                return this;
            }
            String csp = handler.contentSecurityPolicy();
            if (S.notBlank(csp)) {
                H.Response resp = resp();
                resp.addHeaderIfNotAdded(CONTENT_SECURITY_POLICY, csp);
            }
        }
        applyGlobalCspSetting();
        return this;
    }

    private void applyContentType(H.Format fmt) {
        if (null != fmt) {
            ActResponse resp = response;
            resp.initContentType(fmt.contentType());
            resp.commitContentType();
        }
    }

    private void applyGlobalCspSetting() {
        String csp = config().contentSecurityPolicy();
        if (S.blank(csp)) {
            return;
        }
        H.Response r = resp();
        r.addHeaderIfNotAdded(CONTENT_SECURITY_POLICY, csp);
    }

    private void applyGlobalCorsSetting() {
        if (this.disableCors) {
            return;
        }
        AppConfig conf = config();
        if (!conf.corsEnabled()) {
            return;
        }
        H.Response r = response;
        r.addHeaderIfNotAdded(ACCESS_CONTROL_ALLOW_ORIGIN, conf.corsAllowOrigin());
        if (request.method() == H.Method.OPTIONS || !conf.corsOptionCheck()) {
            r.addHeaderIfNotAdded(ACCESS_CONTROL_ALLOW_HEADERS, conf.corsAllowHeaders());
            r.addHeaderIfNotAdded(ACCESS_CONTROL_ALLOW_CREDENTIALS, S.string(conf.corsAllowCredentials()));
            r.addHeaderIfNotAdded(ACCESS_CONTROL_EXPOSE_HEADERS, conf.corsExposeHeaders());
            r.addHeaderIfNotAdded(ACCESS_CONTROL_MAX_AGE, S.string(conf.corsMaxAge()));
        }
    }

    /**
     * Called by bytecode enhancer to set the name list of the render arguments that is update
     * by the enhancer
     *
     * @param names the render argument names separated by ","
     * @return this AppContext
     */
    public ActionContext __appRenderArgNames(String names) {
        return super.__appRenderArgNames(names);
    }

    public ActionContext __controllerInstance(String className, Object instance) {
        if (null == controllerInstances) {
            controllerInstances = new HashMap<>();
        }
        controllerInstances.put(className, instance);
        return this;
    }

    public Object __controllerInstance(String className) {
        return null == controllerInstances ? null : controllerInstances.get(className);
    }


    /**
     * Return cached object by key. The key will be concatenated with
     * current session id when fetching the cached object
     *
     * @param key
     * @param <T> the object type
     * @return the cached object
     */
    public <T> T cached(String key) {
        H.Session sess = session();
        if (null != sess) {
            return sess.cached(key);
        } else {
            return app().cache().get(key);
        }
    }

    /**
     * Add an object into cache by key. The key will be used in conjunction with session id if
     * there is a session instance
     *
     * @param key the key to index the object within the cache
     * @param obj the object to be cached
     */
    public void cache(String key, Object obj) {
        H.Session sess = session();
        if (null != sess) {
            sess.cache(key, obj);
        } else {
            app().cache().put(key, obj);
        }
    }

    /**
     * Add an object into cache by key with expiration time specified
     *
     * @param key        the key to index the object within the cache
     * @param obj        the object to be cached
     * @param expiration the seconds after which the object will be evicted from the cache
     */
    public void cache(String key, Object obj, int expiration) {
        H.Session session = this.session;
        if (null != session) {
            session.cache(key, obj, expiration);
        } else {
            app().cache().put(key, obj, expiration);
        }
    }

    /**
     * Add an object into cache by key and expired after one hour
     *
     * @param key the key to index the object within the cache
     * @param obj the object to be cached
     */
    public void cacheForOneHour(String key, Object obj) {
        cache(key, obj, 60 * 60);
    }

    /**
     * Add an object into cache by key and expired after half hour
     *
     * @param key the key to index the object within the cache
     * @param obj the object to be cached
     */
    public void cacheForHalfHour(String key, Object obj) {
        cache(key, obj, 30 * 60);
    }

    /**
     * Add an object into cache by key and expired after 10 minutes
     *
     * @param key the key to index the object within the cache
     * @param obj the object to be cached
     */
    public void cacheForTenMinutes(String key, Object obj) {
        cache(key, obj, 10 * 60);
    }

    /**
     * Add an object into cache by key and expired after one minute
     *
     * @param key the key to index the object within the cache+
     * @param obj the object to be cached
     */
    public void cacheForOneMinute(String key, Object obj) {
        cache(key, obj, 60);
    }

    /**
     * Evict cached object
     *
     * @param key the key indexed the cached object to be evicted
     */
    public void evictCache(String key) {
        H.Session sess = session();
        if (null != sess) {
            sess.evict(key);
        } else {
            app().cache().evict(key);
        }
    }

    public S.Buffer buildViolationMessage(S.Buffer builder) {
        return buildViolationMessage(builder, "\n");
    }

    public S.Buffer buildViolationMessage(S.Buffer builder, String separator) {
        Map<String, ConstraintViolation> violations = violations();
        if (violations.isEmpty()) return builder;
        for (Map.Entry<String, ConstraintViolation> entry : violations.entrySet()) {
            builder.append(entry.getKey()).append(": ").append(entry.getValue().getMessage()).append(separator);
        }
        int n = builder.length();
        builder.delete(n - separator.length(), n);
        return builder;
    }

    public String violationMessage(String separator) {
        return buildViolationMessage(S.newBuffer(), separator).toString();
    }

    public String violationMessage() {
        return violationMessage("\n");
    }

    public ActionContext flashViolationMessage() {
        return flashViolationMessage("\n");
    }

    public ActionContext flashViolationMessage(String separator) {
        if (violations().isEmpty()) return this;
        flash().error(violationMessage(separator));
        return this;
    }

    public String actionPath() {
        return actionPath;
    }

    public ActionContext actionPath(String path) {
        actionPath = path;
        return this;
    }

    @Override
    public String methodPath() {
        return actionPath;
    }

    public void startIntercepting() {
        state = State.INTERCEPTING;
    }

    public void startHandling() {
        state = State.HANDLING;
    }

    /**
     * Update the context session to mark a user logged in
     * @param username the username
     */
    public void login(String username) {
        session().put(config().sessionKeyUsername(), username);
    }

    /**
     * Login the user and redirect back to original URL
     * @param username
     *      the username
     */
    public void loginAndRedirectBack(String username) {
        login(username);
        RedirectToLoginUrl.redirectToOriginalUrl(this);
    }

    /**
     * Login the user and redirect back to original URL. If no
     * original URL found then redirect to `defaultLandingUrl`.
     *
     * @param username
     *      The username
     * @param defaultLandingUrl
     *      the URL to be redirected if original URL not found
     */
    public void loginAndRedirectBack(String username, String defaultLandingUrl) {
        login(username);
        RedirectToLoginUrl.redirectToOriginalUrl(this, defaultLandingUrl);
    }

    /**
     * Login the user and redirect to specified URL
     * @param username
     *      the username
     * @param url
     *      the URL to be redirected to
     */
    public void loginAndRedirect(String username, String url) {
        login(username);
        throw redirect(url);
    }

    /**
     * Logout the current session. After calling this method,
     * the session will be cleared
     */
    public void logout() {
        SessionManager sessionManager = app().sessionManager();
        sessionManager.logout(session);
    }

    /**
     * Initialize params/renderArgs/attributes and then
     * resolve session and flash from cookies
     */
    public void resolve() {
        E.illegalStateIf(state != State.CREATED);
        localeResolver.resolve();
        boolean sessionFree = handler.sessionFree() || sessionPassThrough;
        setWasUnauthenticated();
        H.Request req = req();
        if (!sessionFree) {
            resolveSession(req);
            app().eventBus().emit(new PreFireSessionResolvedEvent(session, this));
            resolveFlash(req);
        }
        state = State.SESSION_RESOLVED;
        if (!sessionFree) {
            handler.prepareAuthentication(this);
            app().eventBus().emit(new SessionResolvedEvent(session, this));
            if (isLoggedIn()) {
                clearWasUnauthenticated();
            }
        }
    }

    public void proceedWithHandler(RequestHandler handler) {
        /**
         * TODO: fix Dalian-Dong issue
        if (requireBodyParsing) {
            ((RequestImplBase) req()).receiveFullBytesAndProceed(this, handler);
        } else {
            handler.handle(this);
        }
         */
        handler.handle(this);
    }

    @Override
    public Locale locale(boolean required) {
        if (required) {
            if (null == locale()) {
                localeResolver.resolve();
            }
        }
        return super.locale(required);
    }

    public void logAccess(H.Response response) {
        if (null != accessLog) {
            accessLog.logWithResponseCode(response.statusCode());
        }
    }

    /**
     * Dissolve session and flash into cookies.
     * <p><b>Note</b> this method must be called
     * before any content has been committed to
     * response output stream/writer</p>
     */
    public void dissolve() {
        if (state == State.SESSION_DISSOLVED) {
            return;
        }
        if (handler.sessionFree() || sessionPassThrough) {
            return;
        }
        if (null == session) {
            // only case is when CSRF token check failed
            // while resolving session
            // we need to generate new session anyway
            // because it is required to cache the
            // original URL
            // see RedirectToLoginUrl
            session = new H.Session();
        }
        localeResolver.dissolve();
        app().eventBus().emit(new SessionWillDissolveEvent(this));
        try {
            setCsrfCookieAndRenderArgs();
            sessionManager().dissolveState(session(), flash(), resp());
//            dissolveFlash();
//            dissolveSession();
            state = State.SESSION_DISSOLVED;
        } finally {
            app().eventBus().emit(new SessionDissolvedEvent(this));
        }
    }

    /**
     * Clear all internal data store/cache and then
     * remove this context from thread local
     */
    @Override
    protected void releaseResources() {
        super.releaseResources();
        PropertySpec.current.remove();
        if (this.state != State.DESTROYED) {
            sessionManager = null;
            this.allParams = null;
            this.extraParams = null;
            this.requestParamCache = null;
            this.router = null;
            this.handler = null;
            // xio impl might need this this.request = null;
            // xio impl might need this this.response = null;
            this.flash = null;
            this.session = null;
            this.controllerInstances = null;
            this.result = null;
            this.uploads.clear();
            ActionContext.clearLocal();
        }
        this.state = State.DESTROYED;
    }

    @Override
    public Class<? extends Annotation> scope() {
        return RequestScoped.class;
    }

    public void saveLocal() {
        _local.set(this);
    }

    public static void clearLocal() {
        clearCurrent();
    }

    private Set<Map.Entry<String, String[]>> requestParamCache() {
        if (null != requestParamCache) {
            return requestParamCache;
        }
        requestParamCache = new HashSet<>();
        Map<String, String[]> map = new HashMap<>();
        // url queries
        Iterator<String> paramNames = request.paramNames().iterator();
        while (paramNames.hasNext()) {
            final String key = paramNames.next();
            final String[] val = request.paramVals(key);
            MapUtil.mergeValueInMap(map, key, val);
        }
        // post bodies
        Map<String, String[]> map2 = bodyParams();
        for (String key : map2.keySet()) {
            String[] val = map2.get(key);
            if (null != val) {
                MapUtil.mergeValueInMap(map, key, val);
            }
        }
        requestParamCache.addAll(map.entrySet());
        return requestParamCache;
    }

    private void _init() {
        uploads = new HashMap<>();
        extraParams = new HashMap<>();
        final Set<Map.Entry<String, String[]>> paramEntrySet = new AbstractSet<Map.Entry<String, String[]>>() {
            @Override
            public Iterator<Map.Entry<String, String[]>> iterator() {
                final Iterator<Map.Entry<String, String[]>> extraItr = new Iterator<Map.Entry<String, String[]>>() {
                    Iterator<Map.Entry<String, String>> parent = extraParams.entrySet().iterator();

                    @Override
                    public boolean hasNext() {
                        return parent.hasNext();
                    }

                    @Override
                    public Map.Entry<String, String[]> next() {
                        final Map.Entry<String, String> parentEntry = parent.next();
                        return new Map.Entry<String, String[]>() {
                            @Override
                            public String getKey() {
                                return parentEntry.getKey();
                            }

                            @Override
                            public String[] getValue() {
                                return new String[]{parentEntry.getValue()};
                            }

                            @Override
                            public String[] setValue(String[] value) {
                                throw E.unsupport();
                            }
                        };
                    }

                    @Override
                    public void remove() {
                        throw E.unsupport();
                    }
                };
                final Iterator<Map.Entry<String, String[]>> reqParamItr = requestParamCache().iterator();
                return new Iterator<Map.Entry<String, String[]>>() {
                    @Override
                    public boolean hasNext() {
                        return extraItr.hasNext() || reqParamItr.hasNext();
                    }

                    @Override
                    public Map.Entry<String, String[]> next() {
                        if (extraItr.hasNext()) return extraItr.next();
                        return reqParamItr.next();
                    }

                    @Override
                    public void remove() {
                        throw E.unsupport();
                    }
                };
            }

            @Override
            public int size() {
                int size = extraParams.size();
                if (null != request) {
                    size += requestParamCache().size();
                }
                return size;
            }
        };

        allParams = new AbstractMap<String, String[]>() {
            @Override
            public Set<Entry<String, String[]>> entrySet() {
                return paramEntrySet;
            }
        };
    }

    private void resolveSession(H.Request req) {
        preCheckCsrf();
        session = sessionManager().resolveSession(req, this);
        checkCsrf(session);
    }

    private void resolveFlash(H.Request req) {
        //this.flash = Act.sessionManager().resolveFlash(this);
        flash = sessionManager().resolveFlash(req);
    }

    private SessionManager sessionManager() {
        if (null == sessionManager || sessionManager.isDestroyed()) {
            // in case session manager get destroyed during hot reload
            sessionManager = app().sessionManager();
        }
        return sessionManager;
    }

//    private void dissolveSession() {
//        Cookie c = Act.sessionManager().dissolveSession(this);
//        if (null != c) {
//            config().sessionMapper().serializeSession(c, this);
//        }
//    }
//
//    private void dissolveFlash() {
//        Cookie c = Act.sessionManager().dissolveFlash(this);
//        if (null != c) {
//            config().sessionMapper().serializeFlash(c, this);
//        }
//    }
//
    private static ContextLocal<ActionContext> _local = $.contextLocal();

    public static final String METHOD_GET_CURRENT = "current";

    public static ActionContext current() {
        return _local.get();
    }

    public static void clearCurrent() {
        _local.remove();
    }

    /**
     * Create an new {@code AppContext} and return the new instance
     */
    public static ActionContext create(App app, H.Request request, ActResponse<?> resp) {
        return new ActionContext(app, request, resp);
    }

    public enum State {
        CREATED,
        SESSION_RESOLVED,
        SESSION_DISSOLVED,
        INTERCEPTING,
        HANDLING,
        DESTROYED;
        public boolean isHandling() {
            return this == HANDLING;
        }
        public boolean isIntercepting() {
            return this == INTERCEPTING;
        }
    }

    public static class ActionContextEvent extends ActEvent<ActionContext> implements SystemEvent {
        public ActionContextEvent(ActionContext source) {
            super(source);
        }

        public ActionContext context() {
            return source();
        }
    }

    private static class SessionEvent extends ActionContextEvent {
        private H.Session session;

        public SessionEvent(H.Session session, ActionContext source) {
            super(source);
            this.session = session;
        }

        public H.Session session() {
            return session;
        }
    }

    /**
     * This event is fired after session resolved and before
     * context state changed to {@link State#SESSION_RESOLVED}
     * and in turn before {@link SessionResolvedEvent} is fired.
     */
    public static class PreFireSessionResolvedEvent extends SessionEvent {
        public PreFireSessionResolvedEvent(H.Session session, ActionContext context) {
            super(session, context);
        }

        @Override
        public Class<? extends ActEvent<ActionContext>> eventType() {
            return PreFireSessionResolvedEvent.class;
        }
    }

    /**
     * This event is fired after session resolved after
     * all event listeners that listen to the
     * {@link PreFireSessionResolvedEvent} get notified.
     */
    public static class SessionResolvedEvent extends SessionEvent {
        public SessionResolvedEvent(H.Session session, ActionContext context) {
            super(session, context);
        }

        @Override
        public Class<? extends ActEvent<ActionContext>> eventType() {
            return SessionResolvedEvent.class;
        }
    }

    public static class SessionWillDissolveEvent extends ActionContextEvent {
        public SessionWillDissolveEvent(ActionContext source) {
            super(source);
        }

        @Override
        public Class<? extends ActEvent<ActionContext>> eventType() {
            return SessionWillDissolveEvent.class;
        }
    }

    public static class SessionDissolvedEvent extends ActionContextEvent {
        public SessionDissolvedEvent(ActionContext source) {
            super(source);
        }

        @Override
        public Class<? extends ActEvent<ActionContext>> eventType() {
            return SessionDissolvedEvent.class;
        }
    }
}
