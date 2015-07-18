package act.handler.builtin.controller;

import act.app.ActionContext;
import org.osgl.mvc.result.Result;

/**
 * Dispatch request to real controller action method
 */
public class ControllerAction extends ActionHandler<ControllerAction> {

    private ActionHandlerInvoker handlerInvoker;

    public ControllerAction(ActionHandlerInvoker invoker) {
        super(-1);
        this.handlerInvoker = invoker;
    }

    @Override
    public Result handle(ActionContext actionContext) {
        return handlerInvoker.handle(actionContext);
    }

    @Override
    public void accept(Visitor visitor) {
        handlerInvoker.accept(visitor.invokerVisitor());
    }

    @Override
    protected void releaseResources() {
        handlerInvoker.destroy();
        handlerInvoker = null;
    }
}
