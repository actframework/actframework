package testapp.controller;

import act.controller.Controller;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Ok;

/**
 * A Controller declared with context path
 */
@Controller("/foo")
public class WithContextPath {

    @GetAction("/bar")
    public void bar() {
        throw Ok.get();
    }

}
