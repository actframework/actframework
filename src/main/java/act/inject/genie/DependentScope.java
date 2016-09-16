package act.inject.genie;

import act.inject.param.ScopeCacheSupport;
import org.osgl.inject.ScopeCache;

public class DependentScope extends ScopeCacheSupport.Base implements ScopeCache, ScopeCacheSupport {

    public static final DependentScope INSTANCE = new DependentScope();

    @Override
    public <T> T get(Class<T> aClass) {
        return null;
    }

    @Override
    public <T> T get(String key) {
        return null;
    }

    @Override
    public <T> void put(Class<T> aClass, T t) {
    }

    @Override
    public <T> void put(String key, T t) {
    }
}
