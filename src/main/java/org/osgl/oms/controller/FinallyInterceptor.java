package org.osgl.oms.controller;

import org.osgl.mvc.result.Result;
import org.osgl.oms.action.builtin.ActionProxy;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.plugin.Plugin;

/**
 * Intercept request handling after action invoker or if exception raised
 */
public abstract class FinallyInterceptor extends Interceptor<FinallyInterceptor> implements Plugin {

    protected FinallyInterceptor(int priority) {
        super(priority);
    }

    public abstract void handle(AppContext appContext);

    @Override
    public void register() {
        ActionProxy.registerGlobalInterceptor(this);
    }
}
