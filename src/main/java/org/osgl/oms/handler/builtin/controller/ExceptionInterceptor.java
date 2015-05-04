package org.osgl.oms.handler.builtin.controller;

import org.osgl.oms.plugin.Plugin;

public abstract class ExceptionInterceptor
        extends Handler<ExceptionInterceptor>
        implements Plugin, ExceptionInterceptorInvoker {

    protected ExceptionInterceptor(int priority) {
        super(priority);
    }

    @Override
    public void register() {
        RequestHandlerProxy.registerGlobalInterceptor(this);
    }
}
