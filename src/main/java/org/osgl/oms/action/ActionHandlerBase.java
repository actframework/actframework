package org.osgl.oms.action;

import org.osgl._;
import org.osgl.exception.NotAppliedException;
import org.osgl.oms.app.AppContext;

public abstract class ActionHandlerBase extends _.F1<AppContext, Void>  implements ActionHandler {

    @Override
    public Void apply(AppContext context) throws NotAppliedException, _.Break {
        invoke(context);
        return null;
    }

    @Override
    public boolean supportPartialPath() {
        return false;
    }
}
