package testapp.endpoint;

import act.controller.annotation.UrlContext;
import act.security.CSRF;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;

@CSRF.Enable
@UrlContext("/csrf")
public class CSRFTestBed {

    @GetAction
    public void entry() {
    }

    @PostAction("foo")
    public void foo() {
    }

    @PostAction("bar")
    @CSRF.Disable
    public void bar() {
    }

}
