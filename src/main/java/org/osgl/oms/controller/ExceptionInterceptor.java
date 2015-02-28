package org.osgl.oms.controller;

import org.osgl.mvc.result.Result;
import org.osgl.oms.action.builtin.ActionProxy;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.plugin.Plugin;

public abstract class ExceptionInterceptor extends Interceptor<ExceptionInterceptor> implements Plugin {

    protected ExceptionInterceptor(int priority) {
        super(priority);
    }

    public abstract Result handle(Exception e, AppContext appContext);

    @Override
    public void register() {
        ActionProxy.registerGlobalInterceptor(this);
    }
}
