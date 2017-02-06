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
    private boolean sessionFree;
    private boolean requireContextResolving;
    private boolean express;

    public RequestHandlerBase() {
        this.express = this instanceof ExpressHandler;
        this.sessionFree = false;
        this.requireContextResolving = true;
    }

    @Override
    public final Void apply(ActionContext context) throws NotAppliedException, $.Break {
        handle(context);
        return null;
    }

    public RequestHandlerBase setExpress() {
        this.express = true;
        return this;
    }

    @Override
    public boolean express(ActionContext context) {
        return express;
    }

    @Override
    public final Class<? extends Annotation> scope() {
        return ApplicationScoped.class;
    }

    @Override
    public boolean supportPartialPath() {
        return false;
    }

    @Override
    public boolean requireResolveContext() {
        return requireContextResolving;
    }

    public RequestHandlerBase noContextResoving() {
        requireContextResolving = false;
        return this;
    }

    public RequestHandler realHandler() {
        return this;
    }

    public RequestHandlerBase setSessionFree() {
        this.sessionFree = true;
        return this;
    }

    @Override
    public boolean sessionFree() {
        return sessionFree;
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

    public static RequestHandlerBase wrap(final SimpleRequestHandler handler) {
        return new RequestHandlerBase() {
            @Override
            public void handle(ActionContext context) {
                handler.handle(context);
            }
        }.setSessionFree().noContextResoving();
    }
}
