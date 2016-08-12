package act.inject.genie;

import act.app.ActionContext;
import act.app.CliContext;
import act.app.CliSession;
import act.inject.param.ScopeCacheSupport;
import org.osgl.inject.ScopeCache;

public class RequestScope implements ScopeCache.RequestScope, ScopeCacheSupport {

    public static final act.inject.genie.RequestScope INSTANCE = new act.inject.genie.RequestScope();

    @Override
    public <T> T get(Class<T> aClass) {
        return get(aClass.getName());
    }

    @Override
    public <T> T get(String key) {
        ActionContext actionContext = ActionContext.current();
        if (null != actionContext) {
            return actionContext.attribute(key);
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

    public <T> void put(String key, T t) {
        ActionContext actionContext = ActionContext.current();
        if (null != actionContext) {
            actionContext.attribute(key, t);
        }
        CliContext cliContext = CliContext.current();
        if (null != cliContext) {
            CliSession cliSession = cliContext.session();
            cliSession.attribute(key, t);
        }
    }
}
