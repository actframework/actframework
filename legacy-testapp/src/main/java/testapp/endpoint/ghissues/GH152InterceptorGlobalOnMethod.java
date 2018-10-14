package testapp.endpoint.ghissues;

import act.util.Global;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.result.Result;

import static act.controller.Controller.Util.text;

/**
 * Test Global interceptor
 */
public class GH152InterceptorGlobalOnMethod {

    @Global
    @Before(only = "gh152Method")
    public Result intercepted() {
        throw text("intercepted");
    }

}
