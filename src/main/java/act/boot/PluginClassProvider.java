package act.boot;

import act.plugin.Plugin;

import java.util.List;

public interface PluginClassProvider {
    /**
     * Returns a list of classes that contains all {@link Plugin}
     * classes presented in the server and the application
     */
    List<Class<?>> pluginClasses();
}
