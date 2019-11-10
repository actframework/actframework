package ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.PostAction;

@UrlContext("1061")
public class Gh1061 extends BaseController {

    public static class Foo {
        public String name;
    }

    @PostAction
    public Foo test(Foo foo) {
        return foo;
    }

}
