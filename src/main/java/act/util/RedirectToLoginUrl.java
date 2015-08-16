package act.util;

import act.app.ActionContext;
import org.osgl.mvc.result.Redirect;
import org.osgl.mvc.result.Result;

/**
 * When authentication is required but missing, redirect the user to
 * {@link act.conf.AppConfigKey#LOGIN_URL}
 */
public class RedirectToLoginUrl implements MissingAuthenticationHandler {

    private volatile Result R = null;

    @Override
    public Result result(ActionContext context) {
        if (null == R) {
            synchronized (this) {
                if (null == R) {
                    R = new Redirect(context.config().loginUrl());
                }
            }
        }
        return R;
    }
}
