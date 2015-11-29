package testapp.controller;

import org.osgl.mvc.annotation.GetAction;

public class ThrowResultWithParam extends HandlerEnhancerTestController {
    @GetAction("/")
    public void handle(int foo, String bar) {
        throw render(foo, bar);
    }
}
