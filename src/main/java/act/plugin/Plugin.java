package act.plugin;

import org.osgl.util.C;

import java.util.List;

/**
 * Tag a class that could be plug into Act stack
 */
public interface Plugin {
    void register();

    public static class InfoRepo {

        private static List<String> plugins = C.newList();

        public static void register(Plugin plugin) {
            plugins.add(plugin.getClass().getName());
            plugin.register();
        }

        static List<String> plugins() {
            return C.list(plugins);
        }
    }
}
