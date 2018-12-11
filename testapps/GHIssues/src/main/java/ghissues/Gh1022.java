package ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.S;

@UrlContext("1022")
public class Gh1022 extends BaseController {

    public static class Foo {
        public String name = S.random();
    }

    @GetAction("foo/{id}")
    public Foo foo(int id) {
        if (1 == id) {
            return new Foo();
        }
        return null;
    }

    @GetAction("foo/{fooId}/~foo-name~")
    public String fooName(int fooId) {
        if (1 == fooId) {
            return new Foo().name;
        }
        return null;
    }

}
