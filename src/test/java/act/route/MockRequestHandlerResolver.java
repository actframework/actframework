package act.route;

import act.handler.RequestHandler;
import act.handler.RequestHandlerResolverBase;

public class MockRequestHandlerResolver extends RequestHandlerResolverBase {
    @Override
    public RequestHandler resolve(CharSequence payload) {
        return new NamedMockHandler(payload);
    }
}
