package testapp.model;

import act.controller.Controller;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;
import testapp.controller.HandlerEnhancerTestController;

@Controller
public class ModelControllerWithAnnotation extends HandlerEnhancerTestController {
    @GetAction("/")
    public Result handle() {
        return ok();
    }
}
