package org.osgl.oms.route;

import org.osgl.oms.handler.RequestHandler;
import org.osgl.oms.handler.RequestHandlerResolverBase;

public class MockRequestHandlerResolver extends RequestHandlerResolverBase {
    @Override
    public RequestHandler resolve(CharSequence payload) {
        return new NamedMockHandler(payload);
    }
}
