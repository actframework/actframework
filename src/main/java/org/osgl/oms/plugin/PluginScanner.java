package org.osgl.oms.plugin;

import org.osgl._;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.oms.OMS;

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
        List<Class<?>> pluginClasses = OMS.classLoader().pluginClasses();
        int sz = pluginClasses.size();
        for (int i = 0; i < sz; ++i) {
            Class<?> c = pluginClasses.get(i);
            if (Plugin.class.isAssignableFrom(c)) {
                Plugin p = (Plugin)_.newInstance(c);
                p.register();
            }
        }
    }

}
