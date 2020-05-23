package ghissues;

import act.controller.annotation.TemplateContext;
import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;

@UrlContext("1322")
@TemplateContext("1322")
public class Gh1322 extends BaseController {

    public static class Foo {
        public int bar;
    }

    @GetAction
    public void form() {
    }

    @PostAction
    public Foo test(Foo foo) {return foo;}
}
