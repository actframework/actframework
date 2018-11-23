package ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("958")
public class Gh958 extends BaseController {

    @UrlContext("~fooBar~")
    public static class Tester extends Gh958 {

        @GetAction
        public String test() {
            return "ok";
        }

    }


}
