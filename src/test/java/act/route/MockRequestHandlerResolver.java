package act.route;

import act.app.App;
import act.handler.RequestHandler;
import act.handler.RequestHandlerResolverBase;

public class MockRequestHandlerResolver extends RequestHandlerResolverBase {
    @Override
    public RequestHandler resolve(CharSequence payload, App app) {
        return new NamedMockHandler(payload);
    }
}
