package ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("1116/${api.version}")
public class Gh1116 extends BaseController {
    @GetAction
    public String test() {
        return "1116";
    }
}
