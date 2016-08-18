package act.data;

import act.app.ActionContext;
import org.osgl.mvc.util.Binder;
import org.osgl.mvc.util.ParamValueProvider;
import org.osgl.util.E;
import org.osgl.util.S;
import org.osgl.web.util.UserAgent;

/**
 * Resolve file uploads
 */
public class UserAgentBinder extends Binder<UserAgent> {
    @Override
    public UserAgent resolve(UserAgent userAgent, String s, ParamValueProvider paramValueProvider) {
        ActionContext actionContext = ActionContext.current();
        E.illegalStateIf(null == actionContext, "No action context found");
        String ua = actionContext.req().header("User-Agent");
        if (S.blank(ua)) {
            return UserAgent.UNKNOWN;
        }
        return UserAgent.parse(ua);
    }
}
