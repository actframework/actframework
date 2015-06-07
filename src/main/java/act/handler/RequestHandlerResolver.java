package act.handler;

import act.app.App;

public interface RequestHandlerResolver {
    RequestHandler resolve(CharSequence payload, App app);
}
