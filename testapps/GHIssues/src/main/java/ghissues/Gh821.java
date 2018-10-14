package ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("821")
public class Gh821 extends BaseController {

    @GetAction("invalid_json")
    public String getInvalidJSON() {
        return "abc";
    }

    @GetAction("valid_json")
    public String getValidJSON() {
        return "{\"foo\": 123}";
    }

}
