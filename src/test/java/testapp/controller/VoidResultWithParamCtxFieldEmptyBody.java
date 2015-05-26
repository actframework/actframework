package testapp.controller;

import act.app.AppContext;
import act.boot.app.RunApp;
import act.controller.Controller;
import org.osgl.http.H;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;

import static act.controller.Controller.Util.render;
import static act.controller.Controller.Util.text;

@Controller
public class VoidResultWithParamCtxFieldEmptyBody  {
    @Before
    public void mockFormat(String fmt, AppContext context) {
        if ("json".equals(fmt)) {
            context.format(H.Format.json);
        }
        context.session().put("foo", "bar");
    }

    @GetAction("/hello")
    public String sayHello() {
        return "Hello Ying!";
    }

    @GetAction("/bye")
    public void byePlayAndSpring() {
        text("bye Play and Spring!!");
    }

    @GetAction("/greeting")
    public void handle(String who, int age) {
        render(who, age);
    }

    @GetAction("/thank")
    public static String thankYou() {
        return "thank you!";
    }

    public static void main(String[] args) throws Exception {
        RunApp.start(HelloWorldApp.class);
    }
}
