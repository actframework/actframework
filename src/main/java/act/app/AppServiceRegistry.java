package act.app;

import act.Destroyable;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.Map;

class AppServiceRegistry {
    private Map<Class<? extends AppService>, AppService> registry = C.newMap();

    void register(AppService service) {
        E.NPE(service);
        registry.put(service.getClass(), service);
    }

    <T extends AppService<T>> T lookup(Class<T> serviceClass) {
        return (T) registry.get(serviceClass);
    }

    void destroy() {
        for (Destroyable service : registry.values()) {
            service.destroy();
        }
        registry.clear();
    }

}
