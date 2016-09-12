package act.inject.genie;

import act.app.ActionContext;
import act.app.App;
import act.cli.CliContext;
import act.cli.CliSession;
import act.inject.SessionVariable;
import act.inject.param.ScopeCacheSupport;
import org.osgl.http.H;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.ScopeCache;

public class SessionScope extends ScopeCacheSupport.Base implements ScopeCache.SessionScope, ScopeCacheSupport {

    public static final act.inject.genie.SessionScope INSTANCE = new act.inject.genie.SessionScope();
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
            if (null != t) {
                session.cache(key, t, TTL);
            }
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

    @Override
    public String key(BeanSpec spec) {
        SessionVariable sessionVariable = spec.getAnnotation(SessionVariable.class);
        if (null != sessionVariable) {
            return sessionVariable.value();
        }
        return super.key(spec);
    }
}
