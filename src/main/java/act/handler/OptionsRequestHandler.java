package act.handler;

import act.app.ActionContext;
import act.handler.builtin.controller.FastRequestHandler;
import act.handler.event.BeforeCommit;
import act.security.CORS;
import org.osgl.mvc.result.Ok;

public class OptionsRequestHandler extends FastRequestHandler {

    private CORS.Spec corsSpec;

    public OptionsRequestHandler(CORS.Spec corsSpec) {
        this.corsSpec = corsSpec;
    }

    @Override
    public CORS.Spec corsSpec() {
        return this.corsSpec();
    }

    @Override
    public void handle(ActionContext context) {
        corsSpec.applyTo(context);
        context.app().eventBus().trigger(new BeforeCommit(Ok.INSTANCE, context));
    }
}
