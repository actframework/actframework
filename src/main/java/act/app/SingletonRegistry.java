package act.app;

import act.Destroyable;
import act.app.event.AppEventId;
import act.event.AppEventListenerBase;
import org.osgl.$;

import java.util.EventObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * provides service for app to get singleton instance by type
 */
public class SingletonRegistry extends AppServiceBase<SingletonRegistry> {

    private ConcurrentMap<Class<?>, Object> registry = new ConcurrentHashMap<Class<?>, Object>();
    private ConcurrentHashMap<Class<?>, Class<?>> preRegistry = new ConcurrentHashMap<>();
    private boolean batchRegistered = false;

    SingletonRegistry(App app) {
        super(app, false);
    }

    synchronized void register(final Class<?> singletonClass) {
        if (!batchRegistered) {
            if (preRegistry.isEmpty()) {
                app().jobManager().on(AppEventId.DEPENDENCY_INJECTOR_PROVISIONED, "register-singleton-instances", new Runnable() {
                    @Override
                    public void run() {
                        doRegister();
                    }
                }, true);
            }
            preRegistry.put(singletonClass, singletonClass);
        } else {
            register(singletonClass, app().newInstance(singletonClass));
        }
    }

    public void register(Class singletonClass, Object singleton) {
        registry.put(singletonClass, singleton);
    }

    synchronized <T> T get(Class<T> singletonClass) {
        return $.cast(registry.get(singletonClass));
    }

    @Override
    protected void releaseResources() {
        Destroyable.Util.tryDestroyAll(registry.values());
        registry.clear();
    }

    private synchronized void doRegister() {
        batchRegistered = true;
        for (Class<?> c : preRegistry.keySet()) {
            registry.put(c, app().newInstance(c));
        }
        preRegistry.clear();
    }
}
