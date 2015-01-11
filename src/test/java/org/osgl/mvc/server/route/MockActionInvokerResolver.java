package org.osgl.mvc.server.route;

import org.osgl.mvc.server.action.ActionHandler;
import org.osgl.mvc.server.action.ActionHandlerResolverBase;

public class MockActionInvokerResolver extends ActionHandlerResolverBase {
    @Override
    public ActionHandler resolve(CharSequence payload) {
        return new NamedMockHandler(payload);
    }
}
