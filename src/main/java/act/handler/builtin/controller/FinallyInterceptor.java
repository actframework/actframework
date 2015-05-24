package act.handler.builtin.controller;

import act.app.AppContext;
import act.plugin.Plugin;

/**
 * Intercept request handling after action method or if exception raised
 */
public abstract class FinallyInterceptor
        extends Handler<FinallyInterceptor>
        implements Plugin {

    protected FinallyInterceptor(int priority) {
        super(priority);
    }

    public abstract void handle(AppContext appContext);

    @Override
    public void register() {
        RequestHandlerProxy.registerGlobalInterceptor(this);
    }
}
