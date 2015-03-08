package org.osgl.oms.handler.builtin.controller;

import org.osgl.mvc.result.Result;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.util.Prioritised;

public interface AfterInterceptorInvoker extends Prioritised {
    Result handle(Result result, AppContext appContext);
}
