package testapp.controller;

import act.controller.Controller;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;

@Controller
public class ReturnResultWithParamCtxField extends ContextController {
    protected String foo;
    @GetAction("/")
    public Result handle(int foo, String bar) {
        ctx.renderArg("foo", foo).renderArg("bar", bar);
        return render(foo, bar);
    }
}
