package act.handler.builtin;

import act.app.ActionContext;
import act.handler.builtin.controller.FastRequestHandler;
import org.osgl.mvc.result.NotFound;

public class AlwaysNotFound extends FastRequestHandler {

    public static AlwaysNotFound INSTANCE = new AlwaysNotFound();

    @Override
    public void handle(ActionContext context) {
        NotFound.get().apply(context.req(), context.resp());
    }

    @Override
    public String toString() {
        return "error: not found";
    }
}
