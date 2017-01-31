package act.handler.builtin;

import act.app.ActionContext;
import act.handler.ExpressHandler;
import act.handler.builtin.controller.FastRequestHandler;
import org.osgl.mvc.result.MethodNotAllowed;

public class AlwaysMethodNotAllowed extends FastRequestHandler implements ExpressHandler {

    public static AlwaysMethodNotAllowed INSTANCE = new AlwaysMethodNotAllowed();

    @Override
    public void handle(ActionContext context) {
        MethodNotAllowed.get().apply(context.req(), context.resp());
    }

    @Override
    public String toString() {
        return "error: method not allowed";
    }
}
