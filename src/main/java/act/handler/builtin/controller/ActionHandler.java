package act.handler.builtin.controller;

import org.osgl.mvc.result.Result;
import act.app.AppContext;

/**
 * Base class for Action Handler or Before/After interceptor
 */
public abstract class ActionHandler<T extends ActionHandler> extends Handler<T> {

    protected ActionHandler(int priority) {
        super(priority);
    }

    public abstract Result handle(AppContext appContext);
}
