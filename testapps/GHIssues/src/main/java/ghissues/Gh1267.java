package ghissues;

import act.annotations.AllowQrCodeRendering;
import act.app.ActionContext;
import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.S;

@UrlContext("1267")
public class Gh1267 extends BaseController {

    @Before
    public void before(String name, String host, ActionContext context) {
        context.renderArg("email", S.pathConcat(name, '@', host));
    }

    @GetAction
    public String checkBind(String email) {
        return email;
    }

}
