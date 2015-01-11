package org.osgl.mvc.server.undertow;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.HttpString;
import org.osgl.http.H;
import org.osgl.mvc.server.RequestImplBase;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;

public abstract class UndertowRequest extends RequestImplBase<UndertowRequest> {
    @Override
    protected Class<UndertowRequest> _impl() {
        return UndertowRequest.class;
    }

    private HttpServerExchange hse;

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
    protected String _remoteAddr() {
        InetSocketAddress sourceAddress = hse.getSourceAddress();
        if(sourceAddress == null) {
            return "";
        }
        InetAddress address = sourceAddress.getAddress();
        if(address == null) {
            //this is unresolved, so we just return the host name
            //not exactly spec, but if the name should be resolved then a PeerNameResolvingHandler should be used
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
