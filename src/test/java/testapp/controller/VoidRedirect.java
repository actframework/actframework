package testapp.controller;

import act.controller.Controller;
import org.osgl.mvc.annotation.GetAction;

public class VoidRedirect extends HandlerEnhancerTestController {
    @GetAction("/")
    public void handle() {
        Controller.Util.redirect("/abc");
    }
}
