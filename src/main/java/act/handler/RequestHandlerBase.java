package act.handler;

import act.app.ActionContext;
import act.security.CORS;
import act.security.CSRF;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;

import javax.enterprise.context.ApplicationScoped;
import java.lang.annotation.Annotation;

public abstract class RequestHandlerBase extends $.F1<ActionContext, Void> implements RequestHandler {

    private boolean destroyed;

    @Override
    public final Void apply(ActionContext context) throws NotAppliedException, $.Break {
        handle(context);
        return null;
    }

    @Override
    public Class<? extends Annotation> scope() {
        return ApplicationScoped.class;
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
    public CORS.Spec corsSpec() {
        return CORS.Spec.DUMB;
    }

    @Override
    public CSRF.Spec csrfSpec() {
        return CSRF.Spec.DUMB;
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
