package act.handler.builtin.controller;

import act.Destroyable;
import act.app.ActionContext;
import act.util.Prioritised;
import org.osgl.mvc.result.Result;

public interface ExceptionInterceptorInvoker extends Prioritised, Destroyable {
    Result handle(Exception e, ActionContext actionContext);
    void accept(ActionHandlerInvoker.Visitor visitor);
}
