package testapp.controller;

import org.osgl.mvc.annotation.GetAction;

public class StaticThrowResultWithParam extends HandlerEnhancerTestController {
    @GetAction("/")
    public static void handle(int foo, String bar) {
        throw render(foo, bar);
    }
}
