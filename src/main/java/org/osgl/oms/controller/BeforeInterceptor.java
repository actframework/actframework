package org.osgl.oms.controller;

import org.osgl.oms.action.builtin.ActionProxy;
import org.osgl.oms.plugin.Plugin;

/**
 * Intercept request handling before calling to action handler
 */
public abstract class BeforeInterceptor extends ActionInterceptor<BeforeInterceptor> implements Plugin {

    protected BeforeInterceptor(int priority) {
        super(priority);
    }

    @Override
    public void register() {
        ActionProxy.registerGlobalInterceptor(this);
    }
}
