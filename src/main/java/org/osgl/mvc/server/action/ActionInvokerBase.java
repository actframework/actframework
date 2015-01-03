package org.osgl.mvc.server.action;

import org.osgl._;
import org.osgl.exception.NotAppliedException;
import org.osgl.mvc.server.AppContext;
import org.osgl.util.E;

import java.util.HashMap;
import java.util.Map;

public abstract class ActionInvokerBase extends _.F1<AppContext, Void>  implements ActionInvoker {
    protected Map<String, CharSequence> params = new HashMap<String, CharSequence>();

    @Override
    public void setParam(String name, CharSequence val) {
        E.NPE(name);
        params.put(name, val);
    }

    @Override
    public Void apply(AppContext context) throws NotAppliedException, _.Break {
        invoke(context);
        return null;
    }
}
