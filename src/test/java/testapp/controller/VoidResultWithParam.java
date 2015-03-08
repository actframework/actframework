package testapp.controller;

import org.osgl.mvc.annotation.GetAction;

public class VoidResultWithParam extends HandlerEnhancerTestController {
    @GetAction("/")
    public void handle(int foo, String bar) {
        render(foo, bar);
    }
}
