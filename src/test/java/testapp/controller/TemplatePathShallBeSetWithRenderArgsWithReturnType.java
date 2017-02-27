package testapp.controller;

import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;

import static act.controller.Controller.Util.template;

public class TemplatePathShallBeSetWithRenderArgsWithReturnType {

    @GetAction("/1")
    public Result handle(String foo) {
        return template("/template", foo);
    }

}
