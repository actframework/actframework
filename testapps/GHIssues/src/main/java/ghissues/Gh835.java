package ghissues;

import act.app.ActionContext;
import act.controller.Controller;
import act.controller.annotation.UrlContext;
import act.handler.ReturnValueAdvice;
import act.handler.ReturnValueAdvisor;
import org.osgl.aaa.NoAuthentication;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.C;

@UrlContext("835")
@NoAuthentication
public class Gh835 extends Controller.Util {

    public static class GenkoAdvice implements ReturnValueAdvice {
        @Override
        public Object applyTo(Object o, ActionContext actionContext) {
            return C.Map("code", 0, "data", o);
        }
    }

    public static class GlobalAdvice implements ReturnValueAdvice {
        @Override
        public Object applyTo(Object o, ActionContext actionContext) {
            return C.Map("code", 1, "data", o);
        }
    }

    @GetAction("withSpecificAdvice")
    @ReturnValueAdvisor(GenkoAdvice.class)
    public String test1() {
        return "Hello Genko!";
    }

    @GetAction("withGlobalAdvice")
    public Integer test2() {
        return 5;
    }

}
