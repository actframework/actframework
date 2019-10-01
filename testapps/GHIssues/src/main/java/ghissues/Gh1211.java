package ghissues;

import act.app.ActionContext;
import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("1211")
public class Gh1211 extends BaseController {

    @GetAction
    public String test(ActionContext context) {
        return context.i18n("hello %s", "world");
    }

    @GetAction("2")
    public String test2(ActionContext context) {
        return context.i18n("hello {0}", "world");
    }

}
