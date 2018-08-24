package ghissues;

import static act.controller.Controller.Util.template;

import act.Act;
import act.controller.annotation.UrlContext;
import act.util.LogSupport;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("809")
public class Gh809 extends LogSupport {

    @GetAction
    public void index() {
        System.out.println(!Act.isDev());
        template();
    }

}
