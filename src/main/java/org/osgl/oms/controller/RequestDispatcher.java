package org.osgl.oms.controller;

import org.osgl.mvc.result.Result;
import org.osgl.oms.app.AppContext;

/**
 * Dispatch request to real controller action method
 */
public abstract class RequestDispatcher extends ActionInterceptor<RequestDispatcher> {

    protected RequestDispatcher() {
        super(-1);
    }

    public Result dispatch(AppContext context) {
        return handle(context);
    }

}
