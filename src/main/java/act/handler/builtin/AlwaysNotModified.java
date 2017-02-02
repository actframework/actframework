package act.handler.builtin;

import act.app.ActionContext;
import act.handler.ExpressHandler;
import act.handler.builtin.controller.FastRequestHandler;
import org.osgl.mvc.result.NotFound;
import org.osgl.mvc.result.NotModified;

public class AlwaysNotModified extends FastRequestHandler implements ExpressHandler {

    public static AlwaysNotModified INSTANCE = new AlwaysNotModified();

    @Override
    public void handle(ActionContext context) {
        NotModified.get().apply(context.req(), context.resp());
    }

    @Override
    public String toString() {
        return "not modified";
    }
}
