package org.osgl.oms.boot;

import java.util.List;

public interface PluginClassProvider {
    /**
     * Returns a list of classes that contains all {@link org.osgl.oms.plugin.Plugin}
     * classes presented in the server and the application
     */
    List<Class<?>> pluginClasses();
}
