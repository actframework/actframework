package ghissues;

import static act.controller.Controller.Util.template;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("809")
public class Gh809 extends BaseController {

    @GetAction
    public void index() {
        template();
    }

}
