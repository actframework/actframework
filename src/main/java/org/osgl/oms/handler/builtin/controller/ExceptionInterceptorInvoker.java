package org.osgl.oms.handler.builtin.controller;

import org.osgl.mvc.result.Result;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.util.Prioritised;

public interface ExceptionInterceptorInvoker extends Prioritised {
    Result handle(Exception e, AppContext appContext);
}
