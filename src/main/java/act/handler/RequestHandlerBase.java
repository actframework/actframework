package act.handler;

import act.app.ActionContext;
import org.osgl._;
import org.osgl.exception.NotAppliedException;

public abstract class RequestHandlerBase extends _.F1<ActionContext, Void> implements RequestHandler {

    private boolean destroyed;

    @Override
    public final Void apply(ActionContext context) throws NotAppliedException, _.Break {
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

    public RequestHandlerBase realHandler() {
        return this;
    }

    @Override
    public void destroy() {
        if (destroyed) return;
        destroyed = true;
        releaseResources();
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    protected void releaseResources() {}
}
