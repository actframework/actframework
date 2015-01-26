package org.osgl.mvc.server.action.builtin;

import org.osgl.mvc.server.AppContext;
import org.osgl.mvc.server.action.ActionHandlerBase;
import org.osgl.util.E;
import org.osgl.util.S;

public class ControllerProxy extends ActionHandlerBase {

    private String controller;
    private String action;

    public ControllerProxy(String action) {
        int pos = action.lastIndexOf('.');
        final String ERR = "Invalid controller action: %s";
        E.illegalArgumentIf(pos < 0, ERR, action);
        controller = action.substring(0, pos);
        E.illegalArgumentIf(S.isEmpty(controller), ERR, action);
        this.action = action.substring(pos + 1);
        E.illegalArgumentIf(S.isEmpty(this.action), ERR, action);
    }

    public String controller() {
        return controller;
    }

    public String action() {
        return action;
    }

    @Override
    public void invoke(AppContext context) {
        throw E.tbd();
    }

    @Override
    public String toString() {
        return S.fmt("%s.%s", controller, action);
    }
}
