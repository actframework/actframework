package testapp.endpoint.ghissues;

import act.util.Global;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.result.Result;

import static act.controller.Controller.Util.text;

/**
 * Test Global interceptor
 */
@Global
public class GH152Interceptor {

    @Before(only = "gh152")
    public Result intercepted() {
        throw text("intercepted");
    }

}
