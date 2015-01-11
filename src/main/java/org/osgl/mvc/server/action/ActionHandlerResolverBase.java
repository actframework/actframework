package org.osgl.mvc.server.action;

import org.osgl._;
import org.osgl.exception.NotAppliedException;

public abstract class ActionHandlerResolverBase extends _.F1<String, ActionHandler> implements ActionHandlerResolver {

    @Override
    public ActionHandler apply(String s) throws NotAppliedException, _.Break {
        return resolve(s);
    }

}
