package testapp.controller;

import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;
import act.app.ActionContext;

public class ReturnResultWithParamCtxParam extends HandlerEnhancerTestController {
    @GetAction("/")
    public Result handle(int foo, String bar, ActionContext ctx) {
        return render(foo, bar);
    }
}
