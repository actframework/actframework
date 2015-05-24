package act.handler.builtin.controller;

import act.util.Prioritised;
import org.osgl.mvc.result.Result;
import act.app.AppContext;

public interface ActionHandlerInvoker extends Prioritised {
    Result handle(AppContext appContext);
}
