package act.handler;

import act.app.ActionContext;

/**
 * A ｀SimpleRequestHandler` is treated as a {@link RequestHandler} with the following
 * default properties:
 *
 * * {@link RequestHandler#express()}: `true`
 * * {@link RequestHandler#corsSpec()}: {@link act.security.CORS.Spec#DUMB}
 * * {@link RequestHandler#csrfSpec()}: {@link act.security.CSRF.Spec#DUMB}
 * * {@link RequestHandler#sessionFree()}: `true`
 * * {@link RequestHandler#requireResolveContext()}: `false`
 * * {@link RequestHandler#supportPartialPath()}: `false`
 */
public interface SimpleRequestHandler {
    /**
     * Invoke handler upon an action context
     *
     * @param context the context data
     */
    void handle(ActionContext context);
}
