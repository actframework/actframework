package act.handler;

import act.app.ActionContext;
import act.security.CORS;
import org.osgl.util.E;

/**
 * Base class to implement handler delegation chain
 */
public class DelegateRequestHandler extends RequestHandlerBase {
    private RequestHandlerBase handler_;
    protected DelegateRequestHandler(RequestHandlerBase handler) {
        E.NPE(handler);
        this.handler_ = handler;
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
    public boolean sessionFree() {
        return handler_.sessionFree();
    }

    @Override
    public RequestHandlerBase realHandler() {
        return handler_.realHandler();
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
