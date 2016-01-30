package act.handler.builtin.controller;

import act.Destroyable;
import act.app.ActionContext;
import act.util.Prioritised;
import org.osgl.mvc.result.Result;

public interface ActionHandlerInvoker extends Prioritised, Destroyable {
    Result handle(ActionContext actionContext) throws Exception;

    public void accept(Visitor visitor);

    public interface Visitor {}
}
