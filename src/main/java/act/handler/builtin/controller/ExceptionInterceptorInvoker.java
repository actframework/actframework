package act.handler.builtin.controller;

import act.Destroyable;
import act.util.Prioritised;
import org.osgl.mvc.result.Result;
import act.app.AppContext;

public interface ExceptionInterceptorInvoker extends Prioritised, Destroyable {
    Result handle(Exception e, AppContext appContext);
    void accept(ActionHandlerInvoker.Visitor visitor);
}
