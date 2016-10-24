package act.app;

import act.Destroyable;
import org.osgl.$;
import org.osgl.logging.L;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@ApplicationScoped
class AppServiceRegistry {

    private static Logger logger = LogManager.get(AppServiceRegistry.class);

    private Map<Class<? extends AppService>, AppService> registry = C.newMap();
    private C.List<AppService> appendix = C.newList();
    private App app;

    @Inject
    AppServiceRegistry(App app) {
        this.app = $.notNull(app);
    }

    synchronized void register(final AppService service) {
        E.NPE(service);
        final Class<? extends AppService> c = service.getClass();
        if (!registry.containsKey(c)) {
            registry.put(c, service);
            tryRegisterSingletonService(c, service);
        } else {
            E.illegalStateIf(isSingletonService(c), "Singleton AppService[%s] cannot be re-registered", c);
            logger.warn("Service type[%s] already registered", service.getClass());
            appendix.add(service);
        }
    }

    <T extends AppService<T>> T lookup(Class<T> serviceClass) {
        return (T) registry.get(serviceClass);
    }

    // Called when app's singleton registry has been initialized
    synchronized void bulkRegisterSingleton() {
        for (Map.Entry<Class<? extends AppService>, AppService> entry : registry.entrySet()) {
            if (isSingletonService(entry.getKey())) {
                app.registerSingleton(entry.getKey(), entry.getValue());
            }
        }
    }

    void destroy() {
        Destroyable.Util.destroyAll(C.<Destroyable>list(appendix), ApplicationScoped.class);
        Destroyable.Util.destroyAll(C.<Destroyable>list(registry.values()), ApplicationScoped.class);
        appendix.clear();
        registry.clear();
    }

    private boolean isSingletonService(final Class<? extends AppService> c) {
        return c.getAnnotation(Singleton.class) != null;
    }

    private void tryRegisterSingletonService(final Class<? extends AppService> c, final AppService service) {
        if (isSingletonService(c)) {
            app.registerSingleton(c, service);
        }
    }

}
