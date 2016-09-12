package act.cli;

import act.app.ActionContext;
import org.osgl.http.H;

/**
 * The CliOverHttpSession
 */
public class CliOverHttpSession extends CliSession {
    private H.Session session;

    public CliOverHttpSession(ActionContext context) {
        super(context);
        session = context.session();
    }

    @Override
    public CliSession attribute(String key, Object val) {
        session.cache(key, val, app.config().sessionTtl());
        return this;
    }

    @Override
    public <T> T attribute(String key) {
        return session.cached(key);
    }

    @Override
    public CliSession removeAttribute(String key) {
        session.evict(key);
        return this;
    }
}
