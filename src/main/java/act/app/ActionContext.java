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

import act.Act;
import act.Destroyable;
import act.ResponseImplBase;
import act.conf.AppConfig;
import act.controller.ResponseCache;
import act.data.MapUtil;
import act.data.RequestBodyParser;
import act.event.ActEvent;
import act.event.EventBus;
import act.event.SystemEvent;
import act.handler.RequestHandler;
import act.i18n.LocaleResolver;
import act.route.Router;
import act.security.CORS;
import act.util.ActContext;
import act.util.MissingAuthenticationHandler;
import act.util.PropertySpec;
import act.view.RenderAny;
import org.osgl.$;
import org.osgl.concurrent.ContextLocal;
import org.osgl.http.H;
import org.osgl.http.H.Cookie;
import org.osgl.mvc.result.Result;
import org.osgl.storage.ISObject;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;
import org.osgl.web.util.UserAgent;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import java.util.*;

import static act.controller.Controller.Util.*;
import static org.osgl.http.H.Header.Names.*;

/**
 * {@code AppContext} encapsulate contextual properties needed by
 * an application session
 */
@RequestScoped
public class ActionContext extends ActContext.Base<ActionContext> implements Destroyable {

    public static final String ATTR_CSRF_TOKEN = "__csrf__";
    public static final String ATTR_CSR_TOKEN_PREFETCH = "__csrf_prefetch__";
    public static final String ATTR_WAS_UNAUTHENTICATED = "__was_unauthenticated__";
    public static final String ATTR_HANDLER = "__act_handler__";
    public static final String ATTR_PATH_VARS = "__path_vars__";
    public static final String ATTR_RESULT = "__result__";
    public static final String ATTR_EXCEPTION = "__exception__";
    public static final String ATTR_CURRENT_FILE_INDEX = "__file_id__";
    public static final String REQ_BODY = "_body";

    private H.Request request;
    private H.Response response;
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
    private RequestHandler handler;
    private UserAgent ua;
    private String sessionKeyUsername;
    private LocaleResolver localeResolver;
    private boolean disableCors;
    private boolean disableCsrf;
    private Boolean hasTemplate;
    private H.Status forceResponseStatus;
    private boolean cacheEnabled;
    private MissingAuthenticationHandler forceMissingAuthenticationHandler;
    private MissingAuthenticationHandler forceCsrfCheckingFailureHandler;
    private String urlContext;


    @Inject
    private ActionContext(App app, H.Request request, H.Response response) {
        super(app);
        E.NPE(app, request, response);
        request.context(this);
        response.context(this);
        this.request = request;
        this.response = response;
        this._init();
        this.state = State.CREATED;
        AppConfig config = app.config();
        this.disableCors = !config.corsEnabled();
        this.disableCsrf = req().method().safe();
        this.sessionKeyUsername = config.sessionKeyUsername();
        this.localeResolver = new LocaleResolver(this);
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

    public H.Response resp() {
        return response;
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
        this.router = $.notNull(router);
        return this;
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

    public ActionContext urlContext(String context) {
        this.urlContext = context;
        return this;
    }

    public String urlContext() {
        return urlContext;
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
        return super.templatePath(templatePath);
    }

    public RequestHandler handler() {
        return handler;
    }

    public ActionContext handler(RequestHandler handler) {
        E.NPE(handler);
        this.handler = handler;
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

    public String[] paramVals(String name) {
        String val = extraParams.get(name);
        if (null != val) {
            return new String[]{val};
        }
        String[] sa = request.paramVals(name);
        if (null == sa) {
            sa = getBody(name);
        }
        return sa;
    }

    private String[] getBody(String name) {
        Map<String, String[]> body = bodyParams();
        return body.get(name);
    }

    private Map<String, String[]> bodyParams() {
        if (null == bodyParams) {
            synchronized (this) {
                if (null == bodyParams) {
                    Map<String, String[]> map = C.newMap();
                    H.Method method = request.method();
                    if (H.Method.POST == method || H.Method.PUT == method || H.Method.PATCH == method) {
                        RequestBodyParser parser = RequestBodyParser.get(request);
                        map = parser.parse(this);
                    }
                    bodyParams = map;
                }
            }
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
        this.forceResponseStatus = $.notNull(status);
        return this;
    }

    public Result nullValueResult() {
        if (hasRenderArgs()) {
            return new RenderAny();
        }
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

    public ActionContext applyContentType
            () {
        H.Request req = req();
        H.Format fmt = req.accept();
        if (H.Format.UNKNOWN == fmt) {
            fmt = req.contentType();
        }

        ResponseImplBase resp = $.cast(resp());
        if (null != fmt) {
            resp.initContentType(fmt.contentType());
        }
        resp.commitContentType();
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

    private void applyGlobalCorsSetting() {
        if (this.disableCors) {
            return;
        }
        AppConfig conf = config();
        if (!conf.corsEnabled()) {
            return;
        }
        H.Response r = resp();
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
        return renderArg("__arg_names__", C.listOf(names.split(",")));
    }

    public List<String> __appRenderArgNames() {
        return renderArg("__arg_names__");
    }

    public ActionContext __controllerInstance(String className, Object instance) {
        if (null == controllerInstances) {
            controllerInstances = C.newMap();
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
     * Logout the current session. After calling this method,
     * the session will be cleared
     */
    public void logout() {
        session().clear();
    }

    /**
     * Initialize params/renderArgs/attributes and then
     * resolve session and flash from cookies
     */
    public void resolve() {
        E.illegalStateIf(state != State.CREATED);
        boolean sessionFree = handler.sessionFree();
        attribute(ATTR_WAS_UNAUTHENTICATED, true);
        if (!sessionFree) {
            resolveSession();
            resolveFlash();
        }
        localeResolver.resolve();
        state = State.SESSION_RESOLVED;
        if (!sessionFree) {
            handler.prepareAuthentication(this);
            EventBus eventBus = app().eventBus();
            eventBus.emit(new PreFireSessionResolvedEvent(session, this));
            Act.sessionManager().fireSessionResolved(this);
            eventBus.emit(new SessionResolvedEvent(session, this));
            if (isLoggedIn()) {
                attribute(ATTR_WAS_UNAUTHENTICATED, false);
            }
        }
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
        if (handler.sessionFree()) {
            return;
        }
        localeResolver.dissolve();
        app().eventBus().emit(new SessionWillDissolveEvent(this));
        try {
            dissolveFlash();
            dissolveSession();
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
            clearLocal();
            this.uploads.clear();
        }
        this.state = State.DESTROYED;
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

    private void resolveSession() {
        this.session = Act.sessionManager().resolveSession(this);
    }

    private void resolveFlash() {
        this.flash = Act.sessionManager().resolveFlash(this);
    }

    private void dissolveSession() {
        Cookie c = Act.sessionManager().dissolveSession(this);
        if (null != c) {
            config().sessionMapper().serializeSession(c, this);
        }
    }

    private void dissolveFlash() {
        Cookie c = Act.sessionManager().dissolveFlash(this);
        if (null != c) {
            config().sessionMapper().serializeFlash(c, this);
        }
    }

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
    public static ActionContext create(App app, H.Request request, H.Response resp) {
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
     * This event is fired after session resolved and before any
     * {@link act.util.SessionManager.Listener} get called
     */
    public static class PreFireSessionResolvedEvent extends SessionEvent {
        public PreFireSessionResolvedEvent(H.Session session, ActionContext context) {
            super(session, context);
        }
    }

    /**
     * This event is fired after session resolved and after all
     * {@link act.util.SessionManager.Listener} get notified and
     * in turn after all event listeners that listen to the
     * {@link PreFireSessionResolvedEvent} get notified
     */
    public static class SessionResolvedEvent extends SessionEvent {
        public SessionResolvedEvent(H.Session session, ActionContext context) {
            super(session, context);
        }
    }

    public static class SessionWillDissolveEvent extends ActionContextEvent {
        public SessionWillDissolveEvent(ActionContext source) {
            super(source);
        }
    }

    public static class SessionDissolvedEvent extends ActionContextEvent {
        public SessionDissolvedEvent(ActionContext source) {
            super(source);
        }
    }
}
