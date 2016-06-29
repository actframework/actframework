package act.util;

import io.undertow.util.HeaderMap;
import org.osgl.http.H;
import org.osgl.util.C;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static act.util.Protocol.*;

/**
 * Build the request to be sent out
 */
public class RequestBuilder extends H.Request<RequestBuilder> {
    protected UrlBuilder url;
    protected H.Method method;
    protected HeaderMap headers = new HeaderMap();
    protected Map<String, List<String>> params = C.newMap();
    protected Map<String, H.Cookie> cookies = C.newMap();
    protected byte[] body;

    public RequestBuilder(String url) {
        this.url = new UrlBuilder();
    }

    @Override
    protected Class<RequestBuilder> _impl() {
        return RequestBuilder.class;
    }

    @Override
    public H.Method method() {
        return method;
    }

    @Override
    public H.Request method(H.Method method) {
        this.method = method;
        return this;
    }

    @Override
    public String header(String s) {
        return headers.get(s, 0);
    }

    @Override
    public Iterable<String> headers(String s) {
        return headers.get(s);
    }

    @Override
    public String path() {
        return url.path();
    }

    @Override
    public String contextPath() {
        return "";
    }

    @Override
    public String query() {
        return null;
    }

    @Override
    public boolean secure() {
        Protocol p = url.protocol();
        return HTTPS == p || SFTP == p || WSS == p;
    }

    @Override
    protected String _ip() {
        return "127.0.0.1";
    }

    @Override
    protected void _initCookieMap() {
        // nothing need to be done here
    }

    @Override
    protected InputStream createInputStream() {
        if (null == body) {
            return null;
        }
        return new ByteArrayInputStream(body);
    }

    @Override
    public String paramVal(String s) {
        List<String> l = params.get(s);
        return null == l || l.isEmpty() ? null : l.get(0);
    }

    @Override
    public String[] paramVals(String s) {
        List<String> l = params.get(s);
        return null == l ? null : l.toArray(new String[l.size()]);
    }

    @Override
    public Iterable<String> paramNames() {
        return params.keySet();
    }

    public static RequestBuilder get(String url, Object pojo) {
        return null;
    }

}
