package act.inject.param;

import act.inject.genie.RequestScope;
import act.inject.genie.SessionScope;
import act.util.ActContext;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.ScopeCache;

class ScopedParamValueLoader implements ParamValueLoader {
    private ParamValueLoader realLoader;
    private String key;
    private ScopeCacheSupport scopeCache;
    ScopedParamValueLoader(ParamValueLoader loader, BeanSpec beanSpec, ScopeCacheSupport scopeCache) {
        this.realLoader = loader;
        this.key = beanSpec.toString();
        this.scopeCache = scopeCache;
    }

    @Override
    public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
        Object cached = scopeCache.get(key);
        boolean isSession = SessionScope.INSTANCE == scopeCache;
        if (null == cached || isSession) {
            if (isSession) {
                cached = RequestScope.INSTANCE.get(key);
                if (null != cached) {
                    return cached;
                }
            }
            cached = realLoader.load(cached, context, noDefaultValue);
            scopeCache.put(key, cached);
            if (isSession) {
                RequestScope.INSTANCE.put(key, cached);
            }
        }
        return cached;
    }

}
