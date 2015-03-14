package org.osgl.oms.xio.undertow;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.HttpString;
import org.apache.commons.codec.Charsets;
import org.osgl.http.H;
import org.osgl.oms.RequestImplBase;
import org.osgl.util.E;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Deque;
import java.util.Map;

public class UndertowRequest extends RequestImplBase<UndertowRequest> {
    @Override
    protected Class<UndertowRequest> _impl() {
        return UndertowRequest.class;
    }

    private HttpServerExchange hse;
    private Map<String, Deque<String>> queryParams;

    public UndertowRequest(HttpServerExchange exchange) {
        E.NPE(exchange);
        hse = exchange;
    }

    @Override
    protected String _uri() {
        return hse.getRequestPath();
    }

    @Override
    protected H.Method _method() {
        return H.Method.valueOfIgnoreCase(hse.getRequestMethod().toString());
    }

    @Override
    public String header(String name) {
        return hse.getRequestHeaders().get(name, 0);
    }

    @Override
    public Iterable<String> headers(String name) {
        return hse.getRequestHeaders().eachValue(HttpString.tryFromString(name));
    }

    @Override
    public InputStream inputStream() throws IllegalStateException {
        return hse.getInputStream();
    }

    @Override
    public Reader reader() throws IllegalStateException {
        String reqCharset = hse.getRequestCharset();
        Charset cs = null == reqCharset ? Charsets.UTF_8 : Charset.forName(reqCharset);
        return new InputStreamReader(inputStream(), cs);
    }

    @Override
    public String paramVal(String name) {
        if (null == queryParams) {
            queryParams = hse.getQueryParameters();
        }
        Deque<String> dq = queryParams.get(name);
        return null == dq ? null : dq.peekFirst();
    }

    @Override
    public String[] paramVals(String name) {
        if (null == queryParams) {
            queryParams = hse.getQueryParameters();
        }
        Deque<String> dq = queryParams.get(name);
        String[] sa = new String[dq.size()];
        sa = dq.toArray(sa);
        return sa;
    }

    @Override
    public Iterable<String> paramNames() {
        if (null == queryParams) {
            queryParams = hse.getQueryParameters();
        }
        return queryParams.keySet();
    }

    @Override
    protected String _remoteAddr() {
        InetSocketAddress sourceAddress = hse.getSourceAddress();
        if(sourceAddress == null) {
            return "";
        }
        InetAddress address = sourceAddress.getAddress();
        if(address == null) {
            //this is unresolved, so we just return the host className
            //not exactly spec, but if the className should be resolved then a PeerNameResolvingHandler should be used
            //and this is probably better than just returning null
            return sourceAddress.getHostString();
        }
        return address.getHostAddress();
    }

    @Override
    protected void _initCookieMap() {
        Map<String, Cookie> cookies = hse.getRequestCookies();
        if (cookies.isEmpty()) {
            return;
        }
        for (Map.Entry<String, io.undertow.server.handlers.Cookie> entry : cookies.entrySet()) {
            io.undertow.server.handlers.Cookie cookie = entry.getValue();
            try {
                _setCookie(cookie.getName(), CookieConverter.undertow2osgl(cookie));
            } catch (IllegalArgumentException e) {
                // Ignore bad cookie
            }
        }
    }


}
