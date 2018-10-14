package ghissues;

import act.controller.annotation.UrlContext;
import act.inject.param.NoBind;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.N;

import javax.inject.Singleton;

@UrlContext("871")
@Singleton
public class Gh871 extends BaseController {

    public static class Foo {
        public Bar bar = new Bar();
    }

    public static class Bar {
        public int id = N.randInt();
    }

    @NoBind
    private Foo foo;

    @GetAction("setup")
    public Foo setup() {
        foo = new Foo();
        return foo;
    }

    @GetAction
    public Foo test() {
        return foo;
    }

}
