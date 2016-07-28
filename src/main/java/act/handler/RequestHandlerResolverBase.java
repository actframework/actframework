package act.handler;

import act.app.App;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;

import javax.enterprise.context.ApplicationScoped;
import java.lang.annotation.Annotation;

public abstract class RequestHandlerResolverBase extends $.F2<String, App, RequestHandler> implements RequestHandlerResolver {

    private boolean destroyed;

    @Override
    public RequestHandler apply(String s, App app) throws NotAppliedException, $.Break {
        return resolve(s, app);
    }

    @Override
    public void destroy() {
        if (destroyed) return;
        releaseResources();
    }

    @Override
    public Class<? extends Annotation> scope() {
        return ApplicationScoped.class;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    protected void releaseResources() {

    }
}
