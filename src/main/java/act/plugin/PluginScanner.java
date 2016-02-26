package act.plugin;

import act.Act;
import org.osgl.$;
import org.osgl.exception.UnexpectedException;
import org.osgl.logging.L;
import org.osgl.logging.Logger;

import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Responsible for scanning/loading Act server plugins.
 * <p>A server plugin shall be packaged in jar file and put into
 * <code>${ACT_HOME}/plugin folder</code></p>
 */
public class PluginScanner {

    private static final Logger logger = L.get(PluginScanner.class);

    public PluginScanner() {
    }

    public void scan() {
        Plugin.InfoRepo.clear();
        List<Class<?>> pluginClasses = Act.pluginClasses();
        int sz = pluginClasses.size();
        for (int i = 0; i < sz; ++i) {
            Class<?> c = pluginClasses.get(i);
            int modifier = c.getModifiers();
            if (Modifier.isAbstract(modifier) || !Modifier.isPublic(modifier) || c.isInterface()) {
                continue;
            }
            if (Plugin.class.isAssignableFrom(c)) {
                try {
                    Plugin p = (Plugin) $.newInstance(c);
                    Plugin.InfoRepo.register(p);
                } catch (UnexpectedException e) {
                    // ignore: some plugin does not provide default constructor
                }
            }
        }
    }

}
