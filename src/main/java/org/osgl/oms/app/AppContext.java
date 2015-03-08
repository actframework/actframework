package org.osgl.oms.app;

import org.osgl._;
import org.osgl.concurrent.ContextLocal;
import org.osgl.http.H;
import org.osgl.oms.conf.AppConfig;
import org.osgl.util.E;

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
    private Map<String, String[]> allParams;
    private Map<String, Object> attributes;

    private AppContext(App app, H.Request request, H.Response response) {
        E.NPE(app, request, response);
        this.app = app;
        this.request = request;
        this.response = response;
        _init();
    }

    public H.Request req() {
        return request;
    }

    public H.Response resp() {
        return response;
    }

    public H.Session session() {
        return session;
    }

    public H.Flash flash() {
        return flash;
    }

    public H.Format format() {
        return req().format();
    }

    public boolean isJSON() {
        return format() == H.Format.json;
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
        return request.paramVal(name);
    }

    public String[] paramVals(String name) {
        String val = extraParams.get(name);
        if (null != val) {
            return new String[]{val};
        }
        return request.paramVals(name);
    }

    public Map<String, String[]> allParams() {
        return allParams;
    }

    @SuppressWarnings("unchecked")
    public <T> T renderArg(String name) {
        return (T) renderArgs.get(name);
    }

    /**
     * Associate a user attribute to the context. Could be used by third party
     * libraries or user application
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

    public void saveLocal() {
        _local.set(this);
    }

    private Set<Map.Entry<String, String[]>> requestParamCache() {
        if (null != requestParamCache) {
            return requestParamCache;
        }
        requestParamCache = new HashSet<Map.Entry<String, String[]>>();
        Iterator<String> paramNames = request.paramNames().iterator();
        while (paramNames.hasNext()) {
            final String key = paramNames.next();
            final String[] val = request.paramVals(key);
            Map.Entry<String, String[]> entry = new Map.Entry<String, String[]>() {
                @Override
                public String getKey() {
                    return key;
                }

                @Override
                public String[] getValue() {
                    return val;
                }

                @Override
                public String[] setValue(String[] value) {
                    throw E.unsupport();
                }
            };
            requestParamCache.add(entry);
        }
        return requestParamCache;
    }

    private void _init() {
        extraParams = new HashMap<String, String>();
        renderArgs = new HashMap<String, Object>();
        attributes = new HashMap<String, Object>();
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
}
