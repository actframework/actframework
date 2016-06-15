package testapp.endpoint;

import act.controller.Controller;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;

@SuppressWarnings("unused")
public class HelloController extends Controller.Util {

    @GetAction("/hello1")
    public String hello1() {
        return "hello";
    }

    @GetAction("/hello2")
    public Result hello2() {
        return text("hello");
    }

}
