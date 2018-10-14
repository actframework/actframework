package ghissues;

import act.controller.annotation.UrlContext;
import act.util.*;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.N;
import org.osgl.util.S;

@UrlContext("796")
@JsonView
public class Gh796 extends BaseController {

    public static class Foo {
        public String text = S.random();
        public int id = N.randInt();
    }

    @GetAction
    @PropertySpec("-id")
    public Foo foo() {
        return new Foo();
    }

}
