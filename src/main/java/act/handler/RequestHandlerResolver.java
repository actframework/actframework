package act.handler;

import act.Destroyable;
import act.app.App;

public interface RequestHandlerResolver extends Destroyable {
    RequestHandler resolve(CharSequence payload, App app);
}
