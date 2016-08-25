package act.handler.builtin.controller;

import act.Destroyable;
import act.app.ActionContext;
import act.security.CORS;
import act.util.Prioritised;
import org.osgl.mvc.result.Result;

public interface ActionHandlerInvoker extends Prioritised, Destroyable {
    Result handle(ActionContext actionContext) throws Exception;

    void accept(Visitor visitor);

    interface Visitor {}

    boolean sessionFree();

    CORS.Spec corsSpec();
}
