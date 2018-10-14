package ghissues;

import act.controller.annotation.UrlContext;
import act.util.PropertySpec;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.*;

@UrlContext("866")
public class Gh866 extends BaseController {

    public static class Foo {
        public String name = S.random();
        public int number = N.randInt();
    }

    @GetAction
    @PropertySpec("name")
    public Iterable<Foo> list() {
        return C.list(new Foo());
    }

}
