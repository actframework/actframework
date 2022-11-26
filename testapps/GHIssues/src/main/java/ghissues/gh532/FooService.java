package ghissues.gh532;

import act.controller.annotation.UrlContext;
import ghissues.BaseController;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("532")
public class FooService extends BaseController {
    @GetAction
    public void test(Foo foo) {
    }
}
