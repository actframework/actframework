package testapp.controller;

import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;

public class StaticReturnOk extends HandlerEnhancerTestController {
    @GetAction("/")
    public static Result handle() {
        return ok();
    }
}
