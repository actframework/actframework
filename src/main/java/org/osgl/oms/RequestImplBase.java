package org.osgl.oms;

import org.osgl.http.H;
import org.osgl.oms.conf.AppConfig;
import org.osgl.util.FastStr;
import org.osgl.util.S;

public abstract class RequestImplBase<T extends H.Request> extends H.Request<T> {
    private AppConfig cfg;
    private H.Method method;
    private String path;
    private String query;
    private Boolean secure;

    protected abstract String _uri();

    protected abstract H.Method _method();

    protected final T me() {
        return (T) this;
    }

    @Override
    public String contextPath() {
        return cfg.urlContext();
    }

    protected final boolean hasContextPath() {
        String ctxPath = contextPath();
        return S.notBlank(ctxPath);
    }


    @Override
    public H.Method method() {
        if (null == method) {
            method = _method();
        }
        return method;
    }

    @Override
    public String path() {
        if (null == path) {
            parseUri();
        }
        return path;
    }

    @Override
    public String query() {
        if (null == query) {
            parseUri();
        }
        return query;
    }

    @Override
    public boolean secure() {
        if (null == secure) {
            if ("https".equals(cfg.xForwardedProtocol())) {
                secure = true;
            } else {
                secure = parseSecureXHeaders();
            }
        }
        return secure;
    }

    private void parseUri() {
        FastStr fs = FastStr.unsafeOf(_uri());
        if (fs.startsWith("http")) {
            // the uri include the scheme, domain and port
            fs = fs.afterFirst("://"); // strip the scheme
            fs = fs.afterFirst('/'); // strip the domain className, port
            fs = fs.prepend('/'); // attach the '/' to the path
        }
        if (hasContextPath()) {
            fs = fs.after(contextPath());
        }
        path = fs.beforeFirst('?').toString();
        query = fs.afterFirst('?').toString();
    }

    private boolean parseSecureXHeaders() {
        String s = header(H.Header.Names.X_FORWARDED_PROTO);
        if ("https".equals(s)) {
            return true;
        }
        s = header(H.Header.Names.X_FORWARDED_SSL);
        if ("on".equals(s)) {
            return true;
        }
        s = header(H.Header.Names.FRONT_END_HTTPS);
        if ("on".equals(s)) {
            return true;
        }
        s = header(H.Header.Names.X_URL_SCHEME);
        if ("https".equals(s)) {
            return true;
        }
        return false;
    }
}
