package testapp.endpoint;

import act.controller.Controller;
import org.osgl.http.H;
import org.osgl.mvc.annotation.Action;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.mvc.annotation.SessionFree;
import org.osgl.mvc.result.Result;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class HelloController extends Controller.Util {

    @SessionFree
    @GetAction("/hello1")
    public String hello1() {
        return "hello";
    }

    @SessionFree
    @GetAction("/hello2")
    public Result hello2() {
        return text("hello");
    }

    @SessionFree
    @GetAction("/hello3")
    public Map<String, String> hello3() {
        return C.Map("hello", "hello");
    }

    @SessionFree
    @GetAction("/hello4")
    public void hello4() {
        text("hello");
    }

    @SessionFree
    @GetAction("/hello5")
    public void hello5(String toWho) {
        render(toWho);
    }

    @SessionFree
    @PostAction("/hello6")
    public String hello6(int i) {
        return S.string(i);
    }

    @SessionFree
    @Action("/hello/{ids}")
    public int[] helloWithIds(int[] ids) {
        return ids;
    }

    public enum Color {red, green, blue}

    @Action("/hello/color/{colors}")
    public List<Color> helloWithColorList(List<Color> colors) {
        return colors;
    }

    @Action("/hello/c2/{colors}")
    public String helloWithColor(String colors) {
        return colors;
    }

    @GetAction("/req/path")
    public String reqPath(H.Request req) {
        return req.path();
    }

    @GetAction("/req/query")
    public String reqQuery(H.Request req) {
        return req.query();
    }

}
