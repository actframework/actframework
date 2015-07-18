package act.route;

import act.app.ActionContext;
import act.handler.RequestHandler;
import act.handler.RequestHandlerBase;
import org.osgl._;
import org.osgl.http.H;
import org.osgl.util.E;

/**
 * Used to expose Router table for debugging purpose
 */
public class RouteInfo extends _.T3<String, String, String> {
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
