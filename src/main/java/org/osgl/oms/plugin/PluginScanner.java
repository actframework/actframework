package org.osgl.oms.plugin;

import org.osgl._;
import org.osgl.exception.UnexpectedException;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.oms.OMS;

import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Responsible for scanning/loading OMS server plugins.
 * <p>A server plugin shall be packaged in jar file and put into
 * <code>${OMS_HOME}/plugin folder</code></p>
 */
public class PluginScanner {

    private static final Logger logger = L.get(PluginScanner.class);

    public PluginScanner() {
    }

    public void scan() {
        List<Class<?>> pluginClasses = OMS.pluginClasses();
        int sz = pluginClasses.size();
        for (int i = 0; i < sz; ++i) {
            Class<?> c = pluginClasses.get(i);
            int modifier = c.getModifiers();
            if (Modifier.isAbstract(modifier) || !Modifier.isPublic(modifier) || c.isInterface()) {
                continue;
            }
            if (Plugin.class.isAssignableFrom(c)) {
                try {
                    Plugin p = (Plugin) _.newInstance(c);
                    p.register();
                } catch (UnexpectedException e) {
                    // ignore: some plugin does not provide default constructor
                }
            }
        }
    }

}
