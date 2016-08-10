package act.di.param;

import act.di.genie.SessionScope;
import act.util.ActContext;
import org.osgl.inject.BeanSpec;

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
    public Object load(Object bean, ActContext context, boolean noDefaultValue) {
        Object cached = scopeCache.get(key);
        if (null == cached || SessionScope.INSTANCE == scopeCache) {
            cached = realLoader.load(cached, context, noDefaultValue);
            scopeCache.put(key, cached);
        }
        return cached;
    }

}
