package act.app;

import act.Act;
import act.conf.AppConfig;
import act.http.MapUtil;
import act.http.RequestBodyParser;
import act.view.Template;
import org.osgl._;
import org.osgl.concurrent.ContextLocal;
import org.osgl.http.H;
import org.osgl.http.H.Cookie;
import org.osgl.storage.ISObject;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.*;

/**
 * {@code AppContext} encapsulate contextual properties needed by
 * an application session
 */
public class AppContext {

    private App app;
    private H.Request request;
    private H.Response response;
    private H.Session session;
    private H.Flash flash;
    private Set<Map.Entry<String, String[]>> requestParamCache;
    private Map<String, String> extraParams;
    private Map<String, Object> renderArgs;
    private volatile Map<String, String[]> bodyParams;
    private Map<String, String[]> allParams;
    private String actionPath; // e.g. com.mycorp.myapp.controller.AbcController.foo
    private Map<String, Object> attributes;
    private Template template;
    private String templatePath;
    private State state;
    private Map<String, Object> controllerInstances;
    private List<ISObject> uploads;
    private boolean localSaved;

    private AppContext(App app, H.Request request, H.Response response) {
        E.NPE(app, request, response);
        this.app = app;
        this.request = request;
        this.response = response;
        this._init();
        this.state = State.CREATED;
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

    public H.Format accept() {
        return req().accept();
    }

    public AppContext accept(H.Format fmt) {
        req().accept(fmt);
        return this;
    }

    public boolean isJSON() {
        return accept() == H.Format.json;
    }

    public boolean isAjax() {
        return req().isAjax();
    }

    public AppContext param(String name, String value) {
        extraParams.put(name, value);
        return this;
    }

    public String param(String name) {
        String val = extraParams.get(name);
        if (null != val) {
            return val;
        }
        val = request.paramVal(name);
        if (null == val) {
            String[] sa = bodyParams().get(name);
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
            sa = bodyParams().get(name);
        }
        return sa;
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
                }
            }
        }
        return bodyParams;
    }

    public Map<String, String[]> allParams() {
        return allParams;
    }

    public List<ISObject> uploads() {
        return C.list(uploads);
    }

    public AppContext addUpload(ISObject sobj) {
        uploads.add(sobj);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T renderArg(String name) {
        return (T) renderArgs.get(name);
    }

    /**
     * Returns all render arguments
     */
    public Map<String, Object> renderArgs() {
        return C.newMap(renderArgs);
    }

    /**
     * Called by bytecode enhancer to set the name list of the render arguments that is update
     * by the enhancer
     * @param names the render argument names separated by ","
     * @return this AppContext
     */
    public AppContext __appRenderArgNames(String names) {
        renderArgs.put("__arg_names__", C.listOf(names.split(",")));
        return this;
    }

    public List<String> __appRenderArgNames() {
        return (List<String>)renderArgs.get("__arg_names__");
    }

    public AppContext __controllerInstance(String className, Object instance) {
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
    public AppContext attribute(String name, Object attr) {
        attributes.put(name, attr);
        return this;
    }

    public <T> T attribute(String name) {
        return _.cast(attributes.get(name));
    }

    public AppContext renderArg(String name, Object val) {
        renderArgs.put(name, val);
        return this;
    }

    public App app() {
        return app;
    }

    public AppConfig config() {
        return app.config();
    }

    public String actionPath() {
        return actionPath;
    }

    public AppContext actionPath(String path) {
        actionPath = path;
        return this;
    }

    /**
     * Set path to template file
     * @param path the path to template file
     * @return this {@code AppContext}
     */
    public AppContext templatePath(String path) {
        templatePath = path;
        return this;
    }

    /**
     * If {@link #templatePath(String) template path has been set before} then return
     * the template path. Otherwise returns the {@link #actionPath()}
     * @return either template path or action path if template path not set before
     */
    public String templatePath() {
        if (S.notBlank(templatePath)) {
            return templatePath;
        } else {
            return actionPath().replace('.', '/');
        }
    }

    public Template cachedTemplate() {
        return template;
    }

    public AppContext cacheTemplate(Template template) {
        this.template = template;
        return this;
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
        dissolveFlash();
        dissolveSession();
        state = State.SESSION_DISSOLVED;
    }

    /**
     * Clear all internal data store/cache and then
     * remove this context from thread local
     */
    public void destroy() {
        this.allParams = null;
        this.extraParams = null;
        this.requestParamCache = null;
        this.renderArgs = null;
        this.attributes = null;
        this.template = null;
        this.app = null;
        // xio impl might need this this.request = null;
        // xio impl might need this this.response = null;
        this.flash = null;
        this.session = null;
        this.template = null;
        this.state = State.DESTROYED;
        this.controllerInstances = null;
        this.uploads.clear();
        if (localSaved) AppContext.clear();
    }

    public void saveLocal() {
        _local.set(this);
        localSaved = true;
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
        uploads = C.newList();
        extraParams = C.newMap();
        renderArgs = C.newMap();
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
            resp().addCookie(c);
        }
    }

    private void dissolveFlash() {
        Cookie c = Act.sessionManager().dissolveFlash(this);
        if (null != c) {
            resp().addCookie(c);
        }
    }

    private static ContextLocal<AppContext> _local = _.contextLocal();

    public static AppContext get() {
        return _local.get();
    }

    public static void clear() {
        _local.remove();
    }

    /**
     * Create an new {@code AppContext} and return the new instance
     */
    public static AppContext create(App app, H.Request request, H.Response resp) {
        return new AppContext(app, request, resp);
    }

    public enum State {
        CREATED,
        SESSION_RESOLVED,
        SESSION_DISSOLVED,
        DESTROYED
    }
}
