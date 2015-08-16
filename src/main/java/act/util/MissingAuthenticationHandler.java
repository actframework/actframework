package act.util;

import act.app.ActionContext;
import org.osgl.mvc.result.Result;

/**
 * How the framework should respond to request missing authentication
 * while it is required
 */
public interface MissingAuthenticationHandler {
    /**
     * The result to be thrown out when authentication is missing
     */
    Result result(ActionContext context);
}
