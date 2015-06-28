package act.handler.builtin;

import act.app.AppContext;
import act.handler.builtin.controller.FastRequestHandler;
import org.osgl.mvc.result.BadRequest;

public class AlwaysBadRequest extends FastRequestHandler {

    public static AlwaysBadRequest INSTANCE = new AlwaysBadRequest();

    @Override
    public void handle(AppContext context) {
        BadRequest.INSTANCE.apply(context.req(), context.resp());
    }
}
