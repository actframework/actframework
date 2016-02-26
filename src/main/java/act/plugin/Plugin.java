package act.plugin;

import org.osgl.util.C;

import java.util.List;
import java.util.Set;

/**
 * Tag a class that could be plug into Act stack
 */
public interface Plugin {
    void register();

    public static class InfoRepo {

        private static Set<String> plugins = C.newSet();

        public static void register(Plugin plugin) {
            boolean added = plugins.add(plugin.getClass().getName());
            if (added) {
                plugin.register();
            }
        }

        public static void clear() {
            plugins.clear();
        }

        static List<String> plugins() {
            return C.list(plugins);
        }
    }
}
