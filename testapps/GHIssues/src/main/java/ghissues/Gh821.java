package ghissues;

import act.controller.annotation.UrlContext;
import act.util.LogSupport;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("821")
public class Gh821 extends LogSupport {

    @GetAction("invalid_json")
    public String getInvalidJSON() {
        return "abc";
    }

    @GetAction("valid_json")
    public String getValidJSON() {
        return "{\"foo\": 123}";
    }

}
