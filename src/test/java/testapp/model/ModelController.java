package testapp.model;

import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;
import testapp.controller.HandlerEnhancerTestController;

public class ModelController extends HandlerEnhancerTestController {
    @GetAction("/")
    public Result handle() {
        return ok();
    }
}
