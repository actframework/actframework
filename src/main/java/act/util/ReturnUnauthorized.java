package act.util;

import act.app.ActionContext;
import org.osgl.mvc.result.Result;
import org.osgl.mvc.result.Unauthorized;

/**
 * When authentication is required but missing, response with
 * {@link org.osgl.mvc.result.Unauthorized}
 */
public class ReturnUnauthorized implements MissingAuthenticationHandler {
    private static Result R = new Unauthorized();
    @Override
    public Result result(ActionContext context) {
        return R;
    }

    static Result result() {
        return R;
    }
}
