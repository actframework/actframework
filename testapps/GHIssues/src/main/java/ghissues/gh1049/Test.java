package ghissues.gh1049;

import act.controller.annotation.UrlContext;
import ghissues.BaseController;
import org.osgl.mvc.annotation.PostAction;

@UrlContext("1049")
public class Test extends BaseController {

    @PostAction
    public Policy post(Policy policy) {
        return policy;
    }

}
