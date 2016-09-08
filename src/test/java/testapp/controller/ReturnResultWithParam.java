package testapp.controller;

import act.app.ActionContext;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.Param;
import org.osgl.mvc.result.Result;

public class ReturnResultWithParam extends HandlerEnhancerTestController {
    @GetAction("/")
    public Result handle(@Param(value = "who", defIntVal = 1) int foo, @Param(defVal = "foo") String bar) {
        ActionContext.current().renderArg("foo", foo).renderArg("bar", bar);
        return render(foo, bar);
    }
}
