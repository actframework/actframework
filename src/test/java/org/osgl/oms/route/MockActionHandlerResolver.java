package org.osgl.oms.route;

import org.osgl.oms.action.ActionHandler;
import org.osgl.oms.action.ActionHandlerResolverBase;

public class MockActionHandlerResolver extends ActionHandlerResolverBase {
    @Override
    public ActionHandler resolve(CharSequence payload) {
        return new NamedMockHandler(payload);
    }
}
