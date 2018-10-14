package ghissues;

import act.controller.annotation.UrlContext;
import act.util.JsonView;
import org.osgl.mvc.annotation.PostAction;

public abstract class Gh819 extends BaseController {

    public static class Foo {
        public String name;
    }

    @PostAction
    public Foo test(Foo foo) {
        return foo;
    }

    @UrlContext("819")
    public static class Extended extends Gh819 {

        @JsonView
        @Override
        public Foo test(Foo foo) {
            foo.name = "[extended]" + foo.name;
            return foo;
        }
    }


}
