package act.handler.builtin.controller;

import act.Destroyable;
import act.app.AppContext;
import act.util.Prioritised;
import org.osgl.mvc.result.Result;

public interface ExceptionInterceptorInvoker extends Prioritised, Destroyable {
    Result handle(Exception e, AppContext appContext);
    void accept(ActionHandlerInvoker.Visitor visitor);
}
