package testapp.controller;

import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;

public class ReturnOk extends HandlerEnhancerTestController {
    @GetAction("/")
    public Result handle() {
        return ok();
    }
}
