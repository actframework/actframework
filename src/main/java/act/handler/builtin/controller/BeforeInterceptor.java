package act.handler.builtin.controller;

import act.plugin.Plugin;

/**
 * Intercept request handling before calling to action method
 */
public abstract class BeforeInterceptor extends ActionHandler<BeforeInterceptor> implements Plugin {

    public BeforeInterceptor(int priority) {
        super(priority);
    }

    @Override
    public void register() {
        RequestHandlerProxy.registerGlobalInterceptor(this);
    }
}
