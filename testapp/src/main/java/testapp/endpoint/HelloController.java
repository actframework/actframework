package testapp.endpoint;

import act.controller.Controller;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.mvc.result.Result;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.Map;

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

    @GetAction("/hello3")
    public Map<String, String> hello3() {
        return C.map("hello", "hello");
    }

    @GetAction("/hello4")
    public void hello4() {
        text("hello");
    }

    @GetAction("/hello5")
    public void hello5(String toWho) {
        render(toWho);
    }

    @PostAction("/hello6")
    public String hello6(int i) {
        return S.string(i);
    }

}
