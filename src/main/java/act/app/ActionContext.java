package act.app;

import act.Act;
import act.Destroyable;
import act.data.MapUtil;
import act.data.RequestBodyParser;
import act.event.ActEvent;
import act.event.EventBus;
import act.handler.RequestHandler;
import act.route.Router;
import act.util.ActContext;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.osgl.$;
import org.osgl.concurrent.ContextLocal;
import org.osgl.http.H;
import org.osgl.http.H.Cookie;
import org.osgl.mvc.util.ParamValueProvider;
import org.osgl.storage.ISObject;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import javax.validation.ConstraintViolation;
import java.util.*;

/**
 * {@code AppContext} encapsulate contextual properties needed by
 * an application session
 */
public class ActionContext extends ActContext.ActContextBase<ActionContext> implements ActContext<ActionContext>, ParamValueProvider, Destroyable {

    public static final String ATTR_HANDLER = "__act_handler__";
    public static final String REQ_BODY = "_body";

    private String portId;
    private H.Request request;
    private H.Response response;
    private H.Session session;
    private H.Flash flash;
    private Set<Map.Entry<String, String[]>> requestParamCache;
    private Map<String, String> extraParams;
    private volatile Map<String, String[]> bodyParams;
    private volatile JSONObject jsonObject;
    private volatile JSONArray jsonArray;
    private Map<String, String[]> allParams;
    private String actionPath; // e.g. com.mycorp.myapp.controller.AbcController.foo
    private Map<String, Object> attributes;
    private State state;
    private Map<String, Object> controllerInstances;
    private Map<String, ISObject> uploads;
    private Set<ConstraintViolation> violations;
    private Router router;
    private RequestHandler handler;

    private ActionContext(App app, H.Request request, H.Response response) {
        super(app);
        E.NPE(app, request, response);
        this.request = request;
        this.response = response;
        this._init();
        this.state = State.CREATED;
        this.saveLocal();
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

    public H.Flash flash() {
        return flash;
    }

    public Router router() {
        return router;
    }

    public ActionContext router(Router router) {
        this.router = $.notNull(router);
        this.portId = router.portId();
        return this;
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

    public String portId() {
        return portId;
    }

    public Locale locale() {
        return config().localeResolver().resolve(this);
    }

    public boolean isJSON() {
        return accept() == H.Format.JSON;
    }

    public boolean isXML() {
        return accept() == H.Format.XML;
    }

    public boolean isAjax() {
        return req().isAjax();
    }

    public ActionContext param(String name, String value) {
        extraParams.put(name, value);
        return this;
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
        String[] sa = body.get(name);
        if (null != sa) {
            return sa;
        }
        if (body.size() == 1 && body.containsKey(REQ_BODY)) {
            if (null != jsonArray) {
                int len = jsonArray.size();
                sa = new String[len];
                for (int i = 0; i < len; ++i) {
                    sa[i] = S.string(jsonArray.get(i));
                }
            } else if (null != jsonObject) {

            } else {
                sa = body.get(REQ_BODY);
            }
        }
        return sa;
    }

    public Object tryParseJson(String name, Class<?> paramType, Class<?> paramComponentType, int paramCount) {
        if (null != jsonObject) {
            Object o = jsonObject.get(name);
            if (null != o) {
                if (o instanceof JSONObject) {
                    return JSON.parseObject(((JSONObject) o).toJSONString(), paramType);
                } else if (o instanceof JSONArray) {
                    return JSON.parseArray(((JSONArray) o).toJSONString(), paramComponentType);
                } else {
                    return o;
                }
            } else {
                if (Iterable.class.isAssignableFrom(paramType)) {
                    if (List.class.equals(paramType)) {
                        o = C.list();
                    } else if (Set.class.equals(paramType)) {
                        o = C.newSet();
                    }
                } else if (Map.class.equals(paramType)) {
                    o = C.map();
                } else if (paramType.isArray()) {
                    o = new Object[]{};
                }

                // the extra params might already been consumed in field setting
                boolean singleParam = paramCount == 1 || (paramCount - extraParams.size() == 1) ;
                return singleParam ? JSON.parseObject(jsonObject.toJSONString(), paramType) : o;
            }
        } else if (null != jsonArray) {
            paramCount = paramCount - extraParams.size();
            boolean singleParam = paramCount == 1;
            if (!singleParam) {
                return null;
            }
            List list = JSON.parseArray(jsonArray.toJSONString(), paramComponentType);
            if (Iterable.class.isAssignableFrom(paramType)) {
                if (List.class.equals(paramType)) {
                    return list;
                } else if (Set.class.equals(paramType)) {
                    return C.newSet(list);
                }
            }
        }
        return null;
    }

    public JSONArray jsonArray() {
        return jsonArray;
    }

    private Map<String, String[]> bodyParams() {
        if (null == bodyParams) {
            synchronized (this) {
                if (null == bodyParams) {
                    Map<String, String[]> map = C.newMap();
                    H.Method method = request.method();
                    if (H.Method.POST == method || H.Method.PUT == method) {
                        RequestBodyParser parser = RequestBodyParser.get(request);
                        map = parser.parse(this);
                    }
                    bodyParams = map;
                    // try to check if the body is a JSON string
                    if (bodyParams.size() == 1) {
                        String[] sa = bodyParams.get(REQ_BODY);
                        if (null != sa && sa.length == 1) {
                            String s = sa[0].trim();
                            if (s.startsWith("{") && s.endsWith("}")) {
                                jsonObject = (JSONObject) JSON.parse(s);
                            } else if (s.startsWith("[") && s.endsWith("]")) {
                                jsonArray = (JSONArray) JSON.parse(s);
                            }
                        }
                    }
                }
            }
        }
        return bodyParams;
    }

    public Map<String, String[]> allParams() {
        return allParams;
    }

    public ISObject upload(String name) {
        return uploads.get(name);
    }

    public ActionContext addUpload(String name, ISObject sobj) {
        uploads.put(name, sobj);
        return this;
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
     * Associate a user attribute to the context. Could be used by third party
     * libraries or user application
     *
     * @param name the className used to reference the attribute
     * @param attr the attribute object
     * @return this context
     */
    public ActionContext attribute(String name, Object attr) {
        attributes.put(name, attr);
        return this;
    }

    public <T> T attribute(String name) {
        return $.cast(attributes.get(name));
    }

    public <T> T newIntance(String className) {
        return app().newInstance(className, this);
    }

    public <T> T newInstance(Class<? extends T> clazz) {
        if (clazz == ActionContext.class) return $.cast(this);
        return app().newInstance(clazz, this);
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

    public ActionContext addViolations(Set<ConstraintViolation<?>> violations) {
        this.violations.addAll(violations);
        return this;
    }

    public ActionContext addViolation(ConstraintViolation<?> violation) {
        this.violations.add(violation);
        return this;
    }

    public boolean hasViolation() {
        return !violations.isEmpty();
    }

    public Set<ConstraintViolation> violations() {
        return C.set(this.violations);
    }

    public StringBuilder buildViolationMessage(StringBuilder builder) {
        return buildViolationMessage(builder, "\n");
    }

    public StringBuilder buildViolationMessage(StringBuilder builder, String separator) {
        if (violations.isEmpty()) return builder;
        for (ConstraintViolation violation : violations) {
            builder.append(violation.getMessage()).append(separator);
        }
        int n = builder.length();
        builder.delete(n - separator.length(), n);
        return builder;
    }

    public String violationMessage(String separator) {
        return buildViolationMessage(S.builder(), separator).toString();
    }

    public String violationMessage() {
        return violationMessage("\n");
    }

    public ActionContext flashViolationMessage() {
        return flashViolationMessage("\n");
    }

    public ActionContext flashViolationMessage(String separator) {
        if (violations.isEmpty()) return this;
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

    /**
     * If {@link #templatePath(String) template path has been set before} then return
     * the template path. Otherwise returns the {@link #actionPath()}
     *
     * @return either template path or action path if template path not set before
     */
    public String templatePath() {
        String path = super.templatePath();
        if (S.notBlank(path)) {
            return path;
        } else {
            return actionPath().replace('.', '/');
        }
    }

    public void startIntercepting() {
        state = State.INTERCEPTING;
    }

    public void startHandling() {
        state = State.HANDLING;
    }

    /**
     * Initialize params/renderArgs/attributes and then
     * resolve session and flash from cookies
     */
    public void resolve() {
        E.illegalStateIf(state != State.CREATED);
        resolveSession();
        resolveFlash();
        state = State.SESSION_RESOLVED;
        EventBus eventBus = app().eventBus();
        eventBus.emit(new PreFireSessionResolvedEvent(session, this));
        Act.sessionManager().fireSessionResolved(this);
        eventBus.emit(new SessionResolvedEvent(session, this));
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
        if (this.state != State.DESTROYED) {
            this.allParams = null;
            this.extraParams = null;
            this.requestParamCache = null;
            this.attributes.clear();
            this.router = null;
            this.handler = null;
            // xio impl might need this this.request = null;
            // xio impl might need this this.response = null;
            this.flash = null;
            this.session = null;
            this.controllerInstances = null;
            this.violations.clear();
            clearLocal();
            this.uploads.clear();
            for (Object o : this.attributes.values()) {
                if (o instanceof Destroyable) {
                    ((Destroyable) o).destroy();
                }
            }
            this.attributes.clear();
        }
        this.state = State.DESTROYED;
    }

    public void saveLocal() {
        _local.set(this);
    }

    public static void clearLocal() {
        _local.remove();
    }

    private Set<Map.Entry<String, String[]>> requestParamCache() {
        if (null != requestParamCache) {
            return requestParamCache;
        }
        requestParamCache = new HashSet<Map.Entry<String, String[]>>();
        Map<String, String[]> map = C.newMap();
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
        uploads = C.newMap();
        extraParams = C.newMap();
        violations = C.newSet();
        attributes = C.newMap();
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

    public static class ActionContextEvent extends ActEvent<ActionContext> {
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
