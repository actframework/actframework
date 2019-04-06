package ghissues;

import act.controller.Controller;
import act.controller.annotation.UrlContext;
import act.handler.NoReturnValueAdvice;
import act.handler.ReturnValueAdvisor;
import org.osgl.aaa.NoAuthentication;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("835a")
@ReturnValueAdvisor(Gh835.GenkoAdvice.class)
@NoAuthentication
public class Gh835a extends Controller.Util {

    @NoReturnValueAdvice
    @GetAction
    public Integer test() {
        return 5;
    }

}
