package ghissues;

import act.controller.annotation.UrlContext;
import act.util.JsonView;
import org.osgl.mvc.annotation.GetAction;

import java.util.Date;

@UrlContext("797")
@JsonView
public class Gh797 extends BaseController {

    public static class Foo {
        public Date date = new Date();
    }

    @GetAction
    public Foo foo() {
        return new Foo();
    }

}
