package act.app;

import act.Destroyable;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.List;

class ServiceResourceManager {
    private List<Destroyable> services = C.newList();

    void register(Destroyable service) {
        E.NPE(service);
        services.add(service);
    }

    void destroy() {
        for (Destroyable service : services) {
            service.destroy();
        }
        services.clear();
    }

}
