package act.handler.builtin;

import act.app.ActionContext;
import act.handler.builtin.controller.FastRequestHandler;
import org.osgl.mvc.result.BadRequest;

public class AlwaysBadRequest extends FastRequestHandler {

    public static AlwaysBadRequest INSTANCE = new AlwaysBadRequest();

    @Override
    public void handle(ActionContext context) {
        BadRequest.get().apply(context.req(), context.resp());
    }

    @Override
    public String toString() {
        return "error: bad request";
    }
}
