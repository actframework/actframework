package ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("1001")
public class Gh1001 extends BaseController {

    public static class Foo {
        public int id;
        public String name;
    }

    @GetAction
    public Foo test(Foo foo) {
        return foo;
    }

}
