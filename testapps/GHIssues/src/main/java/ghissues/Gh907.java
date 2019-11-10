package ghissues;

import static act.controller.Controller.Util.notFound;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.GetAction;

public class Gh907 {

    public static class Super extends BaseController {
        @Before
        public void before() {
            notFound();
        }
    }

    @UrlContext("907")
    public static class Child extends Super {
        @GetAction
        public String test() {
            return "oops";
        }
    }

}
