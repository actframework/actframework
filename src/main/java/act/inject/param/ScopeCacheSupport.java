package act.inject.param;

import org.osgl.inject.BeanSpec;
import org.osgl.inject.ScopeCache;

public interface ScopeCacheSupport {

    <T> T get(String key);

    <T> void put(String key, T t);

    String key(BeanSpec spec);

    abstract class Base implements ScopeCacheSupport, ScopeCache {

        @Override
        public String key(BeanSpec spec) {
            return spec.toString();
        }
    }
}
