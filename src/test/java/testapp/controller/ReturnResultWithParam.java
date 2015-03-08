package testapp.controller;

import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;

public class ReturnResultWithParam extends HandlerEnhancerTestController {
    @GetAction("/")
    public Result handle(int foo, String bar) {
        return render(foo, bar);
    }
}
