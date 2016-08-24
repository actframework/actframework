package act.handler.builtin.controller;

import act.Destroyable;
import act.app.ActionContext;
import act.util.CORS;
import act.util.Prioritised;
import org.osgl.mvc.result.Result;

public interface ExceptionInterceptorInvoker extends Prioritised, Destroyable {
    Result handle(Exception e, ActionContext actionContext) throws Exception;
    void accept(ActionHandlerInvoker.Visitor visitor);
    CORS.Handler corsHandler();
    boolean sessionFree();
}
