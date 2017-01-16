package act.plugin;

import act.Destroyable;
import act.app.App;
import act.util.DestroyableBase;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;

public class AppServicePluginManager extends DestroyableBase {

    private Map<Class<? extends AppServicePlugin>, AppServicePlugin> registry = new HashMap<>();

    synchronized void register(AppServicePlugin plugin) {
        if (!registry.containsKey(plugin.getClass())) {
            registry.put(plugin.getClass(), plugin);
        }
    }

    public synchronized void applyTo(App app) {
        for (AppServicePlugin plugin : registry.values()) {
            plugin.applyTo(app);
        }
    }

    public <T extends AppServicePlugin> T get(Class<T> key) {
        return (T) registry.get(key);
    }

    @Override
    protected void releaseResources() {
        Destroyable.Util.tryDestroyAll(registry.values(), ApplicationScoped.class);
        registry = null;
    }
}
