package testapp.controller;

import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;

public class ThrowResultWithParam extends HandlerEnhancerTestController {
    @GetAction("/")
    public void handle(int foo, String bar) {
        throw render(foo, bar);
    }
}
