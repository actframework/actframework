package act.handler;

import org.osgl._;
import org.osgl.exception.NotAppliedException;
import act.app.AppContext;

public abstract class RequestHandlerBase extends _.F1<AppContext, Void> implements RequestHandler {

    @Override
    public final Void apply(AppContext context) throws NotAppliedException, _.Break {
        handle(context);
        return null;
    }

    @Override
    public boolean supportPartialPath() {
        return false;
    }

    @Override
    public boolean requireResolveContext() {
        return true;
    }
}
