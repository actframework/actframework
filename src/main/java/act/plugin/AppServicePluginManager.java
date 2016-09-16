package act.plugin;

import act.Destroyable;
import act.app.App;
import act.util.DestroyableBase;
import org.osgl.util.C;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

public class AppServicePluginManager extends DestroyableBase {

    private List<AppServicePlugin> registry = C.newList();

    synchronized void register(AppServicePlugin plugin) {
        if (!registry.contains(plugin)) {
            registry.add(plugin);
        }
    }

    public synchronized void applyTo(App app) {
        for (AppServicePlugin plugin : registry) {
            plugin.applyTo(app);
        }
    }

    @Override
    protected void releaseResources() {
        Destroyable.Util.tryDestroyAll(registry, ApplicationScoped.class);
        registry = null;
    }
}
