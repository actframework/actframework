package act.handler.builtin;

import act.app.AppContext;
import act.handler.builtin.controller.FastRequestHandler;
import org.osgl.mvc.result.Forbidden;
import org.osgl.mvc.result.NotFound;

public class AlwaysForbidden extends FastRequestHandler {

    public static AlwaysForbidden INSTANCE = new AlwaysForbidden();

    @Override
    public void handle(AppContext context) {
        Forbidden.INSTANCE.apply(context.req(), context.resp());
    }
}
