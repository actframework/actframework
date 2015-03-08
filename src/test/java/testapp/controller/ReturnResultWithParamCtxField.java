package testapp.controller;

import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;
import org.osgl.oms.app.AppContext;

public class ReturnResultWithParamCtxField extends ContextController {
    @GetAction("/")
    public Result handle(int foo, String bar) {
        ctx.renderArg("foo", foo).renderArg("bar", bar);
        return render(foo, bar);
    }
}
