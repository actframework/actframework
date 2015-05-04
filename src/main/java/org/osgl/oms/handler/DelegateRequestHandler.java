package org.osgl.oms.handler;

import org.osgl.oms.app.AppContext;
import org.osgl.util.E;

/**
 * Base class to implement handler delegation chain
 */
public class DelegateRequestHandler extends RequestHandlerBase {
    private RequestHandler next;
    protected DelegateRequestHandler(RequestHandler next) {
        E.NPE(next);
        this.next = next;
    }

    @Override
    public void handle(AppContext context) {
        next.handle(context);
    }

    @Override
    public boolean supportPartialPath() {
        return next.supportPartialPath();
    }

    @Override
    public String realPath(String path) {
        return next.realPath(path);
    }
}
