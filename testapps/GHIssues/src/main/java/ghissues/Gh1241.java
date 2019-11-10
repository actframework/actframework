package ghissues;

import act.Act;
import act.app.ActionContext;
import act.controller.annotation.UrlContext;
import act.handler.builtin.FileGetter;
import act.util.Stateless;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("1241")
@Stateless
public class Gh1241 extends BaseController {

    private FileGetter fileGetter = new FileGetter("/", Act.app());

    @GetAction
    public void load(ActionContext context) {
        fileGetter.handle(context);
    }

}
