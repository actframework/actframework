package act.handler.builtin.controller;

import act.app.AppContext;
import org.osgl.mvc.result.Result;

/**
 * Base class for Action Handler or Before/After interceptor
 */
public abstract class ActionHandler<T extends ActionHandler> extends Handler<T> {

    protected ActionHandler(int priority) {
        super(priority);
    }

    public abstract Result handle(AppContext appContext);

}
