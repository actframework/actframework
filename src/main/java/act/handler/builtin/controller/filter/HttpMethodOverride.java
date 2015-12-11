package act.handler.builtin.controller.filter;

import act.app.ActionContext;
import org.osgl.http.H;
import org.osgl.mvc.annotation.Before;

/**
 *
 */
public class HttpMethodOverride {
    @Before
    public void handleHttpMethodOverride(ActionContext context, H.Method _method) {
        H.Request req = context.req();
        if (null != _method) {
            req.method(_method);
        } else {
            String s = req.header(H.Header.Names.X_HTTP_METHOD_OVERRIDE);
            if (null != s) {
                req.method(H.Method.valueOf(s));
            }
        }
    }
}
