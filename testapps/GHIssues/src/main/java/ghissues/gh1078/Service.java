package ghissues.gh1078;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("1078")
public class Service extends Base {

    @GetAction
    public String test() {
        return "okay";
    }

}
