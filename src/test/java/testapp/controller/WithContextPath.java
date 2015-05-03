package testapp.controller;

import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Ok;
import org.osgl.oms.controller.Controller;

/**
 * A Controller declared with context path
 */
@Controller("/foo")
public class WithContextPath {

    @GetAction("/bar")
    public void bar() {
        throw new Ok();
    }

}
