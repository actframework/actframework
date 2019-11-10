package ghissues.gh985;

import act.app.ActionContext;
import act.controller.annotation.UrlContext;
import ghissues.BaseController;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.C;

import java.util.Map;

public abstract class Crud extends BaseController {
    @Before
    public void init(ActionContext context) {
        context.renderArg("superInit", true);
    }

    @GetAction
    public Map test(ActionContext context) {
        Boolean superInit = context.renderArg("superInit");
        Boolean subInit = context.renderArg("subInit");
        return C.Map("superInit", superInit, "subInit", subInit);
    }
}
