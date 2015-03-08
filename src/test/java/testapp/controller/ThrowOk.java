package testapp.controller;

import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;

public class ThrowOk extends HandlerEnhancerTestController {
    @GetAction("/")
    public void handle() {
        throw ok();
    }
}
