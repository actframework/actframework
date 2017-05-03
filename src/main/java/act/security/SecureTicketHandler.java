package act.security;

import act.app.ActionContext;
import act.handler.RequestHandler;
import act.util.DestroyableBase;
import org.osgl.Osgl;
import org.osgl.exception.NotAppliedException;

/**
 * This request handler is responsible for generating a
 * secure ticket which can be used to authenticate a user.
 *
 * A typical use case is once a user logged in the frontend
 * app can request a secure ticket and use it to
 * authenticate a websocket connection
 */
public class SecureTicketHandler extends DestroyableBase implements RequestHandler {
    @Override
    public void handle(ActionContext context) {

    }

    @Override
    public boolean express(ActionContext context) {
        return false;
    }

    @Override
    public boolean supportPartialPath() {
        return false;
    }

    @Override
    public boolean requireResolveContext() {
        return false;
    }

    @Override
    public void prepareAuthentication(ActionContext context) {

    }

    @Override
    public boolean sessionFree() {
        return false;
    }

    @Override
    public CORS.Spec corsSpec() {
        return null;
    }

    @Override
    public CSRF.Spec csrfSpec() {
        return null;
    }

    @Override
    public Void apply(ActionContext context) throws NotAppliedException, Osgl.Break {
        handle(context);
        return null;
    }
}
