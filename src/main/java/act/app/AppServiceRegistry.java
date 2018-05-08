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
import act.event.EventBus;
import org.osgl.$;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
class AppServiceRegistry {

    private static Logger logger = LogManager.get(AppServiceRegistry.class);

    private Map<Class<? extends AppService>, AppService> registry = new HashMap<>();
    private List<AppService> appendix = new ArrayList<>();
    private App app;

    @Inject
    AppServiceRegistry(App app) {
        this.app = $.requireNotNull(app);
    }

    synchronized void register(final AppService service) {
        E.NPE(service);
        final Class<? extends AppService> c = service.getClass();
        if (!registry.containsKey(c)) {
            registry.put(c, service);
            tryRegisterSingletonService(c, service);
        } else {
            E.illegalStateIf(isSingletonService(c), "Singleton AppService[%s] cannot be re-registered", c);
            // we know event bus will get registered twice for the `EventBus.onceBus`
            if (!(service instanceof EventBus)) {
                logger.warn("Service type[%s] already registered", service.getClass());
            }
            appendix.add(service);
        }
    }

    <T extends AppService<T>> T lookup(Class<T> serviceClass) {
        return (T) registry.get(serviceClass);
    }

    // Called when app's singleton registry has been initialized
    synchronized void bulkRegisterSingleton() {
        for (Map.Entry<Class<? extends AppService>, AppService> entry : registry.entrySet()) {
            if (isSingletonService(entry.getKey())) {
                app.registerSingleton(entry.getKey(), entry.getValue());
            }
        }
    }

    void destroy() {
        Destroyable.Util.destroyAll(C.<Destroyable>list(appendix), ApplicationScoped.class);
        Destroyable.Util.destroyAll(C.<Destroyable>list(registry.values()), ApplicationScoped.class);
        appendix.clear();
        registry.clear();
    }

    private boolean isSingletonService(final Class<? extends AppService> c) {
        return c.getAnnotation(Singleton.class) != null;
    }

    private void tryRegisterSingletonService(final Class<? extends AppService> c, final AppService service) {
        if (isSingletonService(c)) {
            app.registerSingleton(c, service);
        }
    }

}
