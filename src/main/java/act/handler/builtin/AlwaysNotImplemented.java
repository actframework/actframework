package act.handler.builtin;

import act.app.ActionContext;
import act.handler.builtin.controller.FastRequestHandler;
import org.osgl.mvc.result.NotImplemented;

public class AlwaysNotImplemented extends FastRequestHandler {

    public static AlwaysNotImplemented INSTANCE = new AlwaysNotImplemented();

    @Override
    public void handle(ActionContext context) {
        NotImplemented.INSTANCE.apply(context.req(), context.resp());
    }

    @Override
    public String toString() {
        return "error: forbidden";
    }
}
