package testapp.controller;

import org.osgl.mvc.annotation.GetAction;

public class VoidOk extends HandlerEnhancerTestController {
    @GetAction("/")
    public void handle() {
        ok();
    }
}
