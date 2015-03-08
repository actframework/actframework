package testapp.controller;

import org.osgl.mvc.annotation.GetAction;

public class StaticThrowOk extends HandlerEnhancerTestController {
    @GetAction("/")
    public static void handle() {
        throw ok();
    }
}
