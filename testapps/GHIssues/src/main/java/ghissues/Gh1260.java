package ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("1260")
public class Gh1260 extends BaseController {

    @GetAction
    public String test() {
        return "Hello World";
    }

    @GetAction("2")
    public String test2() {
        return "<h1>Hello World</h1>";
    }

}
