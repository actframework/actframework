package act.handler;

import act.app.ActionContext;
import act.handler.builtin.controller.FastRequestHandler;
import act.security.CORS;
import org.osgl.http.H;

public class OptionsRequestHandler extends FastRequestHandler implements ExpressHandler {

    private CORS.Spec corsSpec;

    public OptionsRequestHandler(CORS.Spec corsSpec) {
        this.corsSpec = corsSpec;
    }

    @Override
    public CORS.Spec corsSpec() {
        return this.corsSpec;
    }

    @Override
    public void handle(ActionContext context) {
        context.resp().status(H.Status.NO_CONTENT);
        context.applyCorsSpec();
    }
}
