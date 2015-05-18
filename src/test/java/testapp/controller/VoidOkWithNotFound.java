package testapp.controller;

import org.osgl.mvc.annotation.GetAction;

public class VoidOkWithNotFound extends HandlerEnhancerTestController {
    @GetAction("/")
    public void handle(boolean notFound) {
        if (notFound) {
            notFound("blah blah");
        }
        ok();
    }
}
