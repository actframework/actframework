package act.handler.builtin;

import act.app.AppContext;
import act.handler.builtin.controller.FastRequestHandler;
import org.osgl.mvc.result.Forbidden;

public class AlwaysForbidden extends FastRequestHandler {

    public static AlwaysForbidden INSTANCE = new AlwaysForbidden();

    @Override
    public void handle(AppContext context) {
        Forbidden.INSTANCE.apply(context.req(), context.resp());
    }

    @Override
    public String toString() {
        return "error: forbidden";
    }
}
