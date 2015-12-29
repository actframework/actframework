package act.app;

import act.Destroyable;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.Map;

class AppServiceRegistry {

    private static Logger logger = L.get(AppServiceRegistry.class);

    private Map<Class<? extends AppService>, AppService> registry = C.newMap();
    private C.List<AppService> appendix = C.newList();

    synchronized void register(AppService service) {
        E.NPE(service);
        if (!registry.containsKey(service.getClass())) {
            registry.put(service.getClass(), service);
        } else {
            logger.warn("Service type[%s] already registered", service.getClass());
            appendix.add(service);
        }
    }

    <T extends AppService<T>> T lookup(Class<T> serviceClass) {
        return (T) registry.get(serviceClass);
    }

    void destroy() {
        Destroyable.Util.destroyAll(C.<Destroyable>list(appendix));
        Destroyable.Util.destroyAll(C.<Destroyable>list(registry.values()));
        appendix.clear();
        registry.clear();
    }

}
