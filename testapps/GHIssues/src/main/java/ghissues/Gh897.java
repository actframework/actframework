package ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("897")
public class Gh897 extends BaseController {
    @GetAction
    public String test(String x) {
        return x;
    }
}
