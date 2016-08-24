package act.handler;

import act.app.ActionContext;
import act.handler.builtin.controller.FastRequestHandler;
import act.handler.event.BeforeCommit;
import act.util.CORS;
import org.osgl.mvc.result.Ok;

public class OptionHandler extends FastRequestHandler {
    private CORS.Handler corsHandler;

    public OptionHandler(CORS.Handler corsHandler) {
        this.corsHandler = corsHandler;
    }

    @Override
    public CORS.Handler corsHandler() {
        return this.corsHandler();
    }

    @Override
    public void handle(ActionContext context) {
        corsHandler.apply(context);
        context.app().eventBus().trigger(new BeforeCommit(Ok.INSTANCE, context));
    }
}
