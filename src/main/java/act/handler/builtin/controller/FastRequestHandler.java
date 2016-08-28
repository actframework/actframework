package act.handler.builtin.controller;

import act.app.ActionContext;
import act.handler.RequestHandler;
import act.handler.RequestHandlerBase;
import act.security.CSRF;

/**
 * For any handler that does not require the framework to lookup incoming request
 * and construct the sessions, it shall extends from this class
 */
public abstract class FastRequestHandler extends RequestHandlerBase {

    public static final RequestHandler DUMB = new FastRequestHandler() {
        @Override
        public void handle(ActionContext context) {

        }
    };

    @Override
    public boolean requireResolveContext() {
        return false;
    }

    @Override
    public CSRF.Spec csrfSpec() {
        return CSRF.Spec.DUMB;
    }

    @Override
    public boolean sessionFree() {
        return true;
    }
}
