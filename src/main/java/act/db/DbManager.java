package act.db;

import act.util.DestroyableBase;
import org.osgl.$;
import org.osgl.util.C;

import java.util.Map;

public class DbManager extends DestroyableBase {
    private Map<String, DbPlugin> plugins = C.newMap();
    private Map<Class, TimestampGenerator> timestampGeneratorMap = C.newMap();

    synchronized void register(DbPlugin plugin) {
        plugins.put(plugin.getClass().getCanonicalName(), plugin);
    }

    synchronized void register(TimestampGenerator timestampGenerator) {
        timestampGeneratorMap.put(timestampGenerator.timestampType(), timestampGenerator);
    }

    public synchronized DbPlugin plugin(String type) {
        return plugins.get(type);
    }

    public synchronized boolean hasPlugin() {
        return !plugins.isEmpty();
    }

    public synchronized <TIMESTAMP_TYPE> TimestampGenerator<TIMESTAMP_TYPE> timestampGenerator(Class<? extends TIMESTAMP_TYPE> c) {
        return $.cast(timestampGeneratorMap.get(c));
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
