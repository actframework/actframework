package act.plugin;

import act.app.App;
import org.osgl.util.C;

import java.util.List;

public class AppServicePluginManager {

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
}
