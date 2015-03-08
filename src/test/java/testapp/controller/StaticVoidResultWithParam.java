package testapp.controller;

import org.osgl.mvc.annotation.GetAction;

public class StaticVoidResultWithParam extends HandlerEnhancerTestController {
    @GetAction("/")
    public static void handle(int foo, String bar) {
        render(foo, bar);
    }
}
