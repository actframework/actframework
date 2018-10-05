package ghissues;

import act.controller.Controller;
import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("870")
public class Gh870 extends BaseController {

    @GetAction
    public void test() {
        Controller.Util.badRequest(101, "missing key");
    }

}
