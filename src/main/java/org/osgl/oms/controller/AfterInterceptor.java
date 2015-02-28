package org.osgl.oms.controller;

import org.osgl.mvc.result.Result;
import org.osgl.oms.action.builtin.ActionProxy;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.plugin.Plugin;

/**
 * Intercept request handling before calling to action handler
 */
public abstract class AfterInterceptor extends Interceptor<AfterInterceptor> implements Plugin {

    protected AfterInterceptor(int priority) {
        super(priority);
    }

    /**
     * Sub class should implement this method to do further processing on the
     * result and return the processed result, or return a completely new result
     *
     * @param result the generated result by either {@link org.osgl.oms.controller.BeforeInterceptor} or
     *               controller action handler method
     * @param appContext
     * @return the new result been processed
     */
    public Result handle(Result result, AppContext appContext) {
        return result;
    }

    @Override
    public void register() {
        ActionProxy.registerGlobalInterceptor(this);
    }
}
