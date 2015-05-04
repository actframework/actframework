package org.osgl.oms.handler;

import org.osgl._;
import org.osgl.exception.NotAppliedException;
import org.osgl.oms.app.AppContext;

public abstract class RequestHandlerBase extends _.F1<AppContext, Void> implements RequestHandler {

    @Override
    public final Void apply(AppContext context) throws NotAppliedException, _.Break {
        handle(context);
        return null;
    }

    @Override
    public boolean supportPartialPath() {
        return false;
    }

}
