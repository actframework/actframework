package testapp.controller;

import org.osgl.mvc.annotation.GetAction;

import static act.controller.Controller.Util.renderTemplate;

public class TemplatePathShallBeSetWithRenderArgs {

    @GetAction("/2")
    public void handle(String foo) {
        renderTemplate("/template", foo);
    }
}
