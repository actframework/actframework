package testapp.endpoint.ghissues;

import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.result.Result;

import static act.controller.Controller.Util.text;

public class GH136Interceptor {

    @Before
    public Result intercepted() {
        throw text("intercepted");
    }

}
