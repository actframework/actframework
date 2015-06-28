package act.handler.builtin.controller;

import act.Destroyable;
import act.app.AppContext;
import act.util.Prioritised;
import org.osgl.mvc.result.Result;

public interface ActionHandlerInvoker extends Prioritised, Destroyable {
    Result handle(AppContext appContext);

    public void accept(Visitor visitor);

    public interface Visitor {}
}
