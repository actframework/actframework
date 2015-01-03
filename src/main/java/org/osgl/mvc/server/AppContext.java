package org.osgl.mvc.server;

import org.osgl.http.H;
import org.osgl.util.E;

import java.util.HashMap;
import java.util.Map;

public class AppContext {
    private AppConfig config;
    private RequestImplBase request;
    private H.Response response;
    private Map<String, String> extraParams;

    protected AppContext() {
        // for Mock purpose
        extraParams = new HashMap<String, String>();
    }

    public AppContext(AppConfig config, RequestImplBase request, H.Response response) {
        E.NPE(config, request, response);
        this.config = config;
        this.request = request;
        this.request.ctx(this);
        this.response = response;
        extraParams = new HashMap<String, String>();
    }

    public H.Response resp() {
        return response;
    }

    public void param(String name, String value) {
        extraParams.put(name, value);
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

    public AppConfig config() {
        return this.config;
    }
}
