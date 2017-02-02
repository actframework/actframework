package act.handler;

import act.app.ActionContext;
import act.security.CORS;
import act.security.CSRF;
import org.osgl.util.E;

/**
 * Base class to implement handler delegation chain
 */
public class DelegateRequestHandler extends RequestHandlerBase {
    protected RequestHandler handler_;
    private RequestHandler realHandler;
    protected DelegateRequestHandler(RequestHandler handler) {
        E.NPE(handler);
        this.handler_ = handler;
        this.realHandler = handler instanceof RequestHandlerBase ? ((RequestHandlerBase) handler).realHandler() : handler;
    }

    @Override
    public boolean express(ActionContext context) {
        return handler_.express(context);
    }

    @Override
    public void handle(ActionContext context) {
        handler_.handle(context);
    }

    @Override
    public boolean requireResolveContext() {
        return handler_.requireResolveContext();
    }

    @Override
    public boolean supportPartialPath() {
        return handler_.supportPartialPath();
    }

    protected RequestHandler handler() {
        return handler_;
    }

    @Override
    public CORS.Spec corsSpec() {
        return handler_.corsSpec();
    }

    @Override
    public CSRF.Spec csrfSpec() {
        return handler_.csrfSpec();
    }

    @Override
    public boolean sessionFree() {
        return handler_.sessionFree();
    }

    @Override
    public RequestHandler realHandler() {
        return realHandler;
    }

    @Override
    protected void releaseResources() {
        handler_.destroy();
    }

    @Override
    public String toString() {
        return handler().toString();
    }
}
