package org.osgl.mvc.server;

import org.osgl._;
import org.osgl.concurrent.ContextLocal;
import org.osgl.http.H;
import org.osgl.util.E;

import java.util.HashMap;
import java.util.Map;

public class AppContext {
    private AppConfig config;
    private H.Request request;
    private H.Response response;
    private Map<String, String> extraParams;
    private Map<String, Object> renderArgs;

    // for Mock purpose
    protected AppContext() {
        _init();
    }

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
        if (null != request) {
            return request.param(name);
        }
        return null;
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

    private void _init() {
        extraParams = new HashMap<String, String>();
        renderArgs = new HashMap<String, Object>();
    }

    private static ContextLocal<AppContext> _local = _.contextLocal();

    public static AppContext get() {
        return _local.get();
    }

    public static AppContext create(AppConfig config, RequestImplBase request, H.Response resp) {
        return new AppContext(config, request, resp);
    }

    public static void init(AppConfig config, RequestImplBase request, H.Response resp) {
        _local.set(new AppContext(config, request, resp));
    }
}
