package testapp.controller;

import act.app.ActionContext;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;

public class ReturnResultWithParamCtxParam extends HandlerEnhancerTestController {
    @GetAction("/")
    public Result handle(int foo, String bar, ActionContext ctx) {
        return render(foo, bar);
    }
}
