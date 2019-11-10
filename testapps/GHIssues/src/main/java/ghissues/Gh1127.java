package ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.PostAction;

@UrlContext("1127")
public class Gh1127 extends BaseController {

    public static class Foo {
        public int n;
        public float f;
    }

    @PostAction
    public Foo test(Foo foo) {
        return foo;
    }

}
