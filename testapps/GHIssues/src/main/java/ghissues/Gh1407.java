package ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("1407")
public class Gh1407 extends BaseController {

    @Before
    public String before0() {
        return "before0";
    }

    @Before(priority = -1)
    public String before1() {
        return "before1";
    }

    @Before(priority = 1)
    public String before2() {
        return "before2";
    }

    @GetAction
    public String service() {
        return "service";
    }

}
