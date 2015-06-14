package act.handler;

import act.app.App;
import org.osgl._;
import org.osgl.exception.NotAppliedException;

public abstract class RequestHandlerResolverBase extends _.F2<String, App, RequestHandler> implements RequestHandlerResolver {

    private boolean destroyed;

    @Override
    public RequestHandler apply(String s, App app) throws NotAppliedException, _.Break {
        return resolve(s, app);
    }

    @Override
    public void destroy() {
        if (destroyed) return;
        releaseResources();
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    protected void releaseResources() {

    }
}
