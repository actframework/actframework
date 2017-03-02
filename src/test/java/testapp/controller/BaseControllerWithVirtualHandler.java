package testapp.controller;

import act.controller.Controller;
import org.osgl.mvc.annotation.GetAction;

public abstract class BaseControllerWithVirtualHandler extends ControllerBase {

    @GetAction("virtual")
    public void handle() {}

    @Controller("foo")
    public static class Foo extends BaseControllerWithVirtualHandler {
    }
}
