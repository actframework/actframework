package act.handler.builtin;

import act.app.ActionContext;
import act.handler.ExpressHandler;
import act.handler.builtin.controller.FastRequestHandler;
import org.osgl.mvc.result.Forbidden;

public class AlwaysForbidden extends FastRequestHandler implements ExpressHandler {

    public static AlwaysForbidden INSTANCE = new AlwaysForbidden();

    @Override
    public void handle(ActionContext context) {
        Forbidden.get().apply(context.req(), context.resp());
    }

    @Override
    public String toString() {
        return "error: forbidden";
    }
}
