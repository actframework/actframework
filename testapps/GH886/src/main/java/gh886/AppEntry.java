package gh886;

import static act.controller.Controller.Util.renderTemplate;

import act.Act;
import act.app.ActionContext;
import org.osgl.mvc.annotation.GetAction;

public class AppEntry {

    @GetAction
    public void test(ActionContext context, String vo) {
        if (null != vo) {
            context.renderArg("vo", vo);
        }
        renderTemplate("/test");
    }

    public static void main(String[] args) throws Exception {
        Act.start();
    }
}
