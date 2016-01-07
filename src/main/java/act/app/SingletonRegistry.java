package act.app;

import act.Destroyable;
import org.osgl.$;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * provides service for app to get singleton instance by type
 */
class SingletonRegistry extends AppServiceBase<SingletonRegistry> {

    private ConcurrentMap<Class<?>, Object> registry = new ConcurrentHashMap<Class<?>, Object>();

    SingletonRegistry(App app) {
        super(app, false);
    }

    void register(final Class<?> singletonClass) {
        app().jobManager().beforeAppStart(new Runnable() {
            @Override
            public void run() {
                registry.put(singletonClass, app().newInstance(singletonClass));
            }
        });
    }

    <T> T get(Class<T> singletonClass) {
        return $.cast(registry.get(singletonClass));
    }

    @Override
    protected void releaseResources() {
        Destroyable.Util.tryDestroyAll(registry.values());
        registry.clear();
    }
}
