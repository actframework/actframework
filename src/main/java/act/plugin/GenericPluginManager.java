package act.plugin;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.Destroyable;
import act.util.LogSupportedDestroyableBase;
import org.osgl.util.C;

import java.util.*;

public class GenericPluginManager extends LogSupportedDestroyableBase {
    private Map<Class<?>, List<?>> registry = new HashMap<>();

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
            pluginList = new ArrayList<>();
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
