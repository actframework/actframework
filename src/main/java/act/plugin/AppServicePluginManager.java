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
import act.app.App;
import act.util.LogSupportedDestroyableBase;

import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;

public class AppServicePluginManager extends LogSupportedDestroyableBase {

    private Map<Class<? extends AppServicePlugin>, AppServicePlugin> registry = new HashMap<>();

    synchronized void register(AppServicePlugin plugin) {
        if (!registry.containsKey(plugin.getClass())) {
            registry.put(plugin.getClass(), plugin);
        }
    }

    public synchronized void applyTo(App app) {
        for (AppServicePlugin plugin : registry.values()) {
            plugin.applyTo(app);
        }
    }

    public <T extends AppServicePlugin> T get(Class<T> key) {
        return (T) registry.get(key);
    }

    @Override
    protected void releaseResources() {
        Destroyable.Util.tryDestroyAll(registry.values(), ApplicationScoped.class);
        registry = null;
    }
}
