package org.osgl.oms;

import org.osgl._;
import org.osgl.concurrent.ContextLocal;
import org.osgl.http.H;
import org.osgl.util.E;

import java.util.*;

public class AppContext {

    private AppConfig config;
    private H.Request request;
    private H.Response response;
    private Set<Map.Entry<String, String[]>> requestParamCache;
    private Map<String, String> extraParams;
    private Map<String, Object> renderArgs;
    private Map<String, String[]> allParams;

    public AppContext(AppConfig config, H.Request request, H.Response response) {
        E.NPE(config, request, response);
        this.config = config;
        this.request = request;
        this.response = response;
        _init();
    }

    public H.Response resp() {
        return response;
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

    public AppContext renderArg(String name, Object val) {
        renderArgs.put(name, val);
        return this;
    }

    public AppConfig config() {
        return this.config;
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

    public static AppContext create(AppConfig config, H.Request request, H.Response resp) {
        return new AppContext(config, request, resp);
    }

    public static void init(AppConfig config, H.Request request, H.Response resp) {
        _local.set(new AppContext(config, request, resp));
    }
}
