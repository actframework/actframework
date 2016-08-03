package testapp.controller;

import act.controller.Controller;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;

import javax.inject.Named;

@Controller
public class ReturnResultWithParamCtxField extends ContextController {
    //protected String foo;
    @GetAction("/")
    public Result handle(int foo, @Named("zoo") String bar) {
        return render(foo, bar);
    }
}
