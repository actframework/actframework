package ghissues.gh1078;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("1078")
public class Gh1078_Service extends Gh1078_Base {

    @GetAction
    public String test() {
        return "okay";
    }

}
