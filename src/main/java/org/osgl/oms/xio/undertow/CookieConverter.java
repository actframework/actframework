package org.osgl.oms.xio.undertow;

import io.undertow.server.handlers.Cookie;
import org.osgl.http.H;

public enum CookieConverter {
    ;
    public static H.Cookie undertow2osgl(Cookie uc) {
        H.Cookie c = new H.Cookie(uc.getName(), uc.getValue());
        c.domain(uc.getDomain()).httpOnly(uc.isHttpOnly()).path(uc.getPath()).secure(uc.isSecure());
        Integer maxAge = uc.getMaxAge();
        if (null != maxAge) {
            c.maxAge(maxAge);
        }
        return c;
    }
}
