package org.osgl.oms.handler.builtin.controller;

import org.osgl.mvc.result.Result;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.plugin.Plugin;

/**
 * Intercept request handling before calling to action method
 */
public abstract class AfterInterceptor
        extends Handler<AfterInterceptor>
        implements Plugin, AfterInterceptorInvoker {

    protected AfterInterceptor(int priority) {
        super(priority);
    }

    /**
     * Sub class should implement this method to do further processing on the
     * result and return the processed result, or return a completely new result
     *
     * @param result     the generated result by either {@link BeforeInterceptor} or
     *                   controller action handler method
     * @param appContext
     * @return the new result been processed
     */
    @Override
    public Result handle(Result result, AppContext appContext) {
        return result;
    }

    @Override
    public void register() {
        RequestHandlerProxy.registerGlobalInterceptor(this);
    }
}
