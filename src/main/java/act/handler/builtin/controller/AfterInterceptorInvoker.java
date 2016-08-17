package act.handler.builtin.controller;

import act.Destroyable;
import act.app.ActionContext;
import act.util.Prioritised;
import org.osgl.mvc.result.Result;

public interface AfterInterceptorInvoker extends Prioritised, Destroyable {
    Result handle(Result result, ActionContext actionContext) throws Exception;
    void accept(ActionHandlerInvoker.Visitor visitor);
    boolean sessionFree();
}
