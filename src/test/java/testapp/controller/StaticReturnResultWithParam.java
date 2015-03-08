package testapp.controller;

import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;

public class StaticReturnResultWithParam extends HandlerEnhancerTestController {
    @GetAction("/")
    public static Result handle(int foo, String bar) {
        return render(foo, bar);
    }
}
