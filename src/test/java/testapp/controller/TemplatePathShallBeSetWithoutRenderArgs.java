package testapp.controller;

import org.osgl.mvc.annotation.GetAction;

import static act.controller.Controller.Util.renderTemplate;

public class TemplatePathShallBeSetWithoutRenderArgs {

    @GetAction("/1")
    public void handle() {
        renderTemplate("/template");
    }

}
