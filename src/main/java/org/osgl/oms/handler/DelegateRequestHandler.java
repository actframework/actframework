package org.osgl.oms.handler;

import org.osgl.oms.app.AppContext;
import org.osgl.util.E;

/**
 * Base class to implement handler delegation chain
 */
public class DelegateRequestHandler extends RequestHandlerBase {
    private RequestHandler handler_;
    protected DelegateRequestHandler(RequestHandler handler) {
        E.NPE(handler);
        this.handler_ = handler;
    }

    @Override
    public void handle(AppContext context) {
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

}
