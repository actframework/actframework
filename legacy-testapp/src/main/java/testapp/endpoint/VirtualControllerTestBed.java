package testapp.endpoint;

import static act.controller.Controller.Util.renderText;

import act.controller.annotation.UrlContext;
import act.util.Virtual;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("/vc")
public class VirtualControllerTestBed {

    @GetAction("1")
    @Virtual
    public void handler1() {
        renderText("1" + getClass().getSimpleName());
    }

    @GetAction("2")
    public void handler2() {
        renderText("2" + getClass().getSimpleName());
    }

    @UrlContext("foo")
    public static class Foo extends VirtualControllerTestBed {}

    @UrlContext("bar")
    public static class Bar extends VirtualControllerTestBed {}

}
