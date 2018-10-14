package ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("860")
public class Gh860 extends BaseController {

    @GetAction
    public String test(String content) {
        return content;
    }

}
