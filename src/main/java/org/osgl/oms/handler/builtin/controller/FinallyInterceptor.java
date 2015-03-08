package org.osgl.oms.handler.builtin.controller;

import org.osgl.oms.app.AppContext;
import org.osgl.oms.plugin.Plugin;

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
