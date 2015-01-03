package org.osgl.mvc.server.route;

import org.osgl.mvc.server.action.ActionInvoker;
import org.osgl.mvc.server.action.ActionInvokerResolverBase;

public class MockActionInvokerResolver extends ActionInvokerResolverBase {
    @Override
    public ActionInvoker resolve(CharSequence name) {
        return new MockInvoker(name);
    }
}
