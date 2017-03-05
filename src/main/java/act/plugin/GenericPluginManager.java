package act.plugin;

import act.Destroyable;
import act.util.DestroyableBase;
import org.osgl.util.C;

import java.util.List;
import java.util.Map;

public class GenericPluginManager extends DestroyableBase {
    private Map<Class<?>, List<?>> registry = C.newMap();

    @Override
    protected void releaseResources() {
        for (List<?> pluginList : registry.values()) {
            for (Object plugin: pluginList) {
                if (plugin instanceof Destroyable) {
                    ((Destroyable) plugin).destroy();
                }
            }
        }
        registry.clear();
    }

    public synchronized <CT, ET extends CT> GenericPluginManager register(Class<CT> clazz, ET plugin) {
        List pluginList = registry.get(clazz);
        if (null == pluginList) {
            pluginList = C.newList();
            registry.put(clazz, pluginList);
        }
        pluginList.add(plugin);
        return this;
    }

    public <T> List<T> pluginList(Class<T> pluginClass) {
        List<T> list = (List<T>)registry.get(pluginClass);
        return null == list ? C.<T>list() : C.<T>list(list);
    }

}
