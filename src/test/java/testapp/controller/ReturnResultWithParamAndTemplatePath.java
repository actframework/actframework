package testapp.controller;

import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;

public class ReturnResultWithParamAndTemplatePath extends HandlerEnhancerTestController {
    @GetAction("/")
    public Result handle(int foo, String bar) {
        return render("/path/to/template", foo, bar);
    }
}
