package org.osgl.mvc.server.action;

import org.osgl.mvc.server.AppContext;

public interface ActionInvoker {
    void invoke(AppContext context);
    void setParam(String name, CharSequence val);
}
