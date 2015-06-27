package act.db;

import act.util.DestroyableBase;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.Map;

public class DbManager extends DestroyableBase {
    private Map<String, DbPlugin> plugins = C.newMap();

    synchronized void register(DbPlugin plugin) {
        plugins.put(plugin.getClass().getCanonicalName(), plugin);
    }

    public synchronized DbPlugin plugin(String type) {
        return plugins.get(type);
    }

    public synchronized boolean hasPlugin() {
        return !plugins.isEmpty();
    }

    /**
     * Returns the plugin if there is only One plugin inside
     * the register, otherwise return {@code null}
     */
    public synchronized DbPlugin theSolePlugin() {
        if (plugins.size() == 1) {
            return plugins.values().iterator().next();
        } else {
            return null;
        }
    }
}
