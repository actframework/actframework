package act.handler.builtin;

import act.app.ActionContext;
import act.handler.ExpressHandler;
import act.handler.builtin.controller.FastRequestHandler;
import org.osgl.mvc.result.NotImplemented;

public class AlwaysNotImplemented extends FastRequestHandler implements ExpressHandler {

    public static AlwaysNotImplemented INSTANCE = new AlwaysNotImplemented();

    @Override
    public void handle(ActionContext context) {
        NotImplemented.get().apply(context.req(), context.resp());
    }

    @Override
    public String toString() {
        return "error: forbidden";
    }
}
