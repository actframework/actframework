package ghissues.gh985;

import act.app.ActionContext;
import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.Before;

@UrlContext("985")
public class Sub extends Crud {
    @Before
    public void initial(ActionContext context) {
        context.renderArg("subInit", true);
    }
}