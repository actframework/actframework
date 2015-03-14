package org.osgl.oms.xio.undertow;

import io.undertow.server.handlers.Cookie;
import org.osgl.http.H;

import java.util.Date;

public enum CookieConverter {
    ;
    public static H.Cookie undertow2osgl(Cookie uc) {
        H.Cookie c = new H.Cookie(uc.getName(), uc.getValue());
        c.domain(uc.getDomain()).httpOnly(uc.isHttpOnly())
                .path(uc.getPath()).secure(uc.isSecure())
                .version(uc.getVersion()).comment(uc.getComment());
        Integer maxAge = uc.getMaxAge();
        if (null != maxAge) {
            c.maxAge(maxAge);
        }
        Date exp = uc.getExpires();
        if (null != exp) {
            c.expires(exp);
        }
        return c;
    }

    public static Cookie osgl2undertow(H.Cookie hc) {
        return new UndertowCookieAdaptor(hc);
    }
}
