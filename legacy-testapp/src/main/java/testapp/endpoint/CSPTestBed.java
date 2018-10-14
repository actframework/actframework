package testapp.endpoint;

import act.controller.annotation.UrlContext;
import act.security.CSP;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("/csp")
@CSP("default-src 'self'; img-src https://*; child-src 'none';")
public class CSPTestBed {

    public static final String GLOBAL_SETTING = "default-src 'self'";
    public static final String CONTROLLER_SETTING = "default-src 'self'; img-src https://*; child-src 'none';";
    public static final String FOO_METHOD_SETTING = "default-src 'self'; child-src 'none';";

    @GetAction
    public void entry() {
    }

    @GetAction("foo")
    @CSP("default-src 'self'; child-src 'none';")
    public void foo() {
    }

    @GetAction("bar")
    @CSP.Disable
    public void bar() {
    }

}
