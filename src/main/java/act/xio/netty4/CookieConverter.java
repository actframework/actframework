package act.xio.netty4;

import io.netty.handler.codec.http.Cookie;
import org.osgl.http.H;

enum CookieConverter {
    ;

    public static H.Cookie netty2osgl(Cookie nc) {
        return new H.Cookie(nc.getName(), nc.getValue(), (int) nc.getMaxAge(), nc.isSecure(), nc.getPath(), nc.getDomain(), nc.isHttpOnly());
    }
}
