package testapp.endpoint;

import act.controller.Controller;
import act.util.Virtual;
import org.osgl.mvc.annotation.GetAction;

import static act.controller.Controller.Util.renderText;

@Controller("/vc")
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

    @Controller("foo")
    public static class Foo extends VirtualControllerTestBed {}

    @Controller("bar")
    public static class Bar extends VirtualControllerTestBed {}

}
