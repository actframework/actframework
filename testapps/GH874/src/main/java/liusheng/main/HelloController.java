package liusheng.main;

import act.controller.Controller;
import org.osgl.mvc.annotation.GetAction;

public class HelloController {

    public String hello1() {
        return "SUCCESS";
    }
    public String hello2() {
        return "SUCCESS";
    }

    @GetAction("/hello3")
    public String hello3() {
        return "SUCCESS";
    }

    public void  hello4() {
        Controller.Util.renderText("哈哈哈 输出 %s","Hello World");

    }


}
