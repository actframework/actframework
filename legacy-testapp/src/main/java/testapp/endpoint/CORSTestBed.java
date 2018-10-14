package testapp.endpoint;

import act.controller.annotation.UrlContext;
import act.security.CORS;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PutAction;

@CORS.AllowHeaders(CORSTestBed.ALLOW_HEADERS)
@CORS.AllowOrigin(CORSTestBed.ALLOW_ORIGIN)
@UrlContext("/cors")
@SuppressWarnings("unused")
public class CORSTestBed {

    public static final String ALLOW_HEADERS = "X-Header-007";
    public static final String ALLOW_ORIGIN = "actframework.org";

    public static final String FOO_ALLOW_ORIGIN = "baidu.com";

    @CORS.AllowOrigin(FOO_ALLOW_ORIGIN)
    @GetAction("foo")
    public static void foo() {
    }

    @CORS.Disable
    @PutAction("bar")
    public static void bar() {
    }
}
