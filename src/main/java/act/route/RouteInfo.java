package act.route;

import act.app.ActionContext;
import act.handler.RequestHandler;
import act.handler.RequestHandlerBase;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.util.E;
import org.osgl.util.S;

/**
 * Used to expose Router table for debugging purpose
 */
public class RouteInfo extends $.T3<String, String, String> implements Comparable<RouteInfo> {
    public RouteInfo(H.Method method, String path, RequestHandler handler) {
        super(method.name(), path, handler.toString());
    }
    public String method() {
        return _1;
    }
    public String path() {
        return _2;
    }
    public String handler() {
        return _3;
    }
    public String compactHandler() {
        String[] sa = _3.split("\\.");
        int len = sa.length;
        if (len == 1) {
            return _3;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len - 2; ++i) {
            sb.append(sa[i].charAt(0)).append('.');
        }
        sb.append(sa[len - 2]).append('.').append(sa[len - 1]);
        return sb.toString();
    }

    @Override
    public String toString() {
        return S.fmt("[%s %s] -> [%s]", method(), path(), compactHandler());
    }

    @Override
    public int compareTo(RouteInfo routeInfo) {
        int n = path().compareTo(routeInfo.path());
        if (n != 0) {
            return n;
        }
        n = method().compareTo(routeInfo.method());
        if (n != 0) {
            return n;
        }
        return handler().compareTo(routeInfo.handler());
    }

    public static RouteInfo of(ActionContext context) {
        H.Method m = context.req().method();
        String path = context.req().url();
        RequestHandler handler = context.handler();
        if (null == handler) {
            handler = UNKNOWN_HANDLER;
        }
        return new RouteInfo(m, path, handler);
    }

    public static final RequestHandler UNKNOWN_HANDLER = new RequestHandlerBase() {
        @Override
        public void handle(ActionContext context) {
            throw E.unsupport();
        }

        @Override
        public String toString() {
            return "unknown";
        }
    };
}
