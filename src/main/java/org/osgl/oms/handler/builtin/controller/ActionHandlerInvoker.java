package org.osgl.oms.handler.builtin.controller;

import org.osgl._;
import org.osgl.mvc.result.Result;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.util.Prioritised;

import java.awt.image.RescaleOp;

public interface ActionHandlerInvoker extends Prioritised {
    Result handle(AppContext appContext);
}
