package act.di.genie;

import act.app.ActionContext;
import act.app.App;
import act.app.CliContext;
import act.app.CliSession;
import act.di.param.ScopeCacheSupport;
import org.osgl.http.H;
import org.osgl.inject.ScopeCache;

public class SessionScope implements ScopeCache.SessionScope, ScopeCacheSupport {

    public static final act.di.genie.SessionScope INSTANCE = new act.di.genie.SessionScope();
    private final int TTL;

    public SessionScope() {
        TTL = (int) App.instance().config().sessionTtl();
    }

    @Override
    public <T> T get(Class<T> aClass) {
        return get(aClass.getName());
    }

    @Override
    public <T> T get(String key) {
        ActionContext actionContext = ActionContext.current();
        if (null != actionContext) {
            H.Session session = actionContext.session();
            T t = session.cached(key);
            session.cache(key, t, TTL);
            return t;
        }
        CliContext cliContext = CliContext.current();
        if (null != cliContext) {
            CliSession cliSession = cliContext.session();
            return cliSession.attribute(key);
        }
        return null;
    }

    @Override
    public <T> void put(Class<T> aClass, T t) {
        put(aClass.getName(), t);
    }

    @Override
    public <T> void put(String key, T t) {
        ActionContext actionContext = ActionContext.current();
        if (null != actionContext) {
            actionContext.session().cache(key, t, TTL);
        }
        CliContext cliContext = CliContext.current();
        if (null != cliContext) {
            CliSession cliSession = cliContext.session();
            cliSession.attribute(key, t);
        }
    }
}
