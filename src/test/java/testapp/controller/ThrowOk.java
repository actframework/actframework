package testapp.controller;

import org.osgl.mvc.annotation.GetAction;

public class ThrowOk extends HandlerEnhancerTestController {
    @GetAction("/")
    public void handle() {
        throw ok();
    }
}
