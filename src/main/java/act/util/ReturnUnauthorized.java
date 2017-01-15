package act.util;

import act.Act;
import act.app.ActionContext;
import act.view.ActUnauthorized;
import org.osgl.mvc.result.Result;
import org.osgl.mvc.result.Unauthorized;

/**
 * When authentication is required but missing, response with
 * {@link org.osgl.mvc.result.Unauthorized}
 */
public class ReturnUnauthorized implements MissingAuthenticationHandler {
    private static Result R = Unauthorized.get();
    @Override
    public Result result(ActionContext context) {
        return Act.isDev() ? ActUnauthorized.create() : R;
    }

    static Result result() {
        return Act.isDev() ? ActUnauthorized.create() : R;
    }
}
