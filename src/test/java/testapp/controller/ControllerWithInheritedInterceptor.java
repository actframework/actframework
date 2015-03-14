package testapp.controller;

import org.osgl.mvc.annotation.GetAction;

public class ControllerWithInheritedInterceptor extends FilterAB {
    @GetAction("/")
    public void handle() {

    }
}
