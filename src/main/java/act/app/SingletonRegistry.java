package act.app;

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
import act.app.event.SysEventId;
import org.osgl.$;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * provides service for app to get singleton instance by type
 */
public class SingletonRegistry extends AppServiceBase<SingletonRegistry> {

    private ConcurrentMap<Class<?>, Object> registry = new ConcurrentHashMap<Class<?>, Object>();
    private ConcurrentHashMap<Class<?>, Class<?>> preRegistry = new ConcurrentHashMap<>();
    private boolean batchRegistered = false;

    SingletonRegistry(App app) {
        super(app, false);
    }

    synchronized void register(final Class<?> singletonClass) {
        if (!batchRegistered) {
            if (preRegistry.isEmpty()) {
                app().jobManager().on(SysEventId.DEPENDENCY_INJECTOR_PROVISIONED, "register-singleton-instances", new Runnable() {
                    @Override
                    public void run() {
                        doRegister();
                    }
                }, true);
            }
            preRegistry.put(singletonClass, singletonClass);
        } else {
            register(singletonClass, app().getInstance(singletonClass));
        }
    }

    public void register(Class singletonClass, Object singleton) {
        registry.put(singletonClass, singleton);
    }

    <T> T get(Class<T> singletonClass) {
        return $.cast(registry.get(singletonClass));
    }

    @Override
    protected void releaseResources() {
        Destroyable.Util.tryDestroyAll(registry.values(), ApplicationScoped.class);
        registry.clear();
    }

    private void doRegister() {
        batchRegistered = true;
        for (Map.Entry<Class<?>, Class<?>> entry: preRegistry.entrySet()) {
            Class<?> c = entry.getKey();
            registry.put(c, app().getInstance(c));
        }
        preRegistry.clear();
    }
}
