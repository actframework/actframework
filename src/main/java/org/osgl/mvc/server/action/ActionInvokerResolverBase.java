package org.osgl.mvc.server.action;

import org.osgl._;
import org.osgl.exception.NotAppliedException;

public abstract class ActionInvokerResolverBase extends _.F1<String, ActionInvoker> implements ActionInvokerResolver {

    @Override
    public ActionInvoker apply(String s) throws NotAppliedException, _.Break {
        return resolve(s);
    }

}
