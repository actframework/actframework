package act.inject.param;

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
import act.app.AppServiceBase;
import act.inject.DependencyInjector;
import act.inject.genie.GenieInjector;
import act.util.ActContext;
import act.util.DestroyableBase;
import act.util.SingletonBase;
import org.osgl.inject.BeanSpec;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ProvidedValueLoader extends DestroyableBase implements ParamValueLoader {
    private DependencyInjector<?> injector;
    private BeanSpec beanSpec;
    private Object singleton;
    private ProvidedValueLoader(BeanSpec beanSpec, DependencyInjector<?> injector) {
        Class type = beanSpec.rawType();
        if (AppServiceBase.class.isAssignableFrom(type)
                || SingletonBase.class.isAssignableFrom(type)
                || type.isAnnotationPresent(Singleton.class)
                || type.isAnnotationPresent(ApplicationScoped.class)) {
            singleton = injector.get(type);
        }
        this.beanSpec = beanSpec;
        this.injector = injector;
    }

    @Override
    public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
        if (null != singleton) {
            return singleton;
        }
        if (null != context && context.getClass().equals(beanSpec.rawType())) {
            return context;
        } else {
            GenieInjector genieInjector = (GenieInjector) injector;
            return genieInjector.get(beanSpec);
        }
    }

    @Override
    public String bindName() {
        return beanSpec.name();
    }

    @Override
    protected void releaseResources() {
        Destroyable.Util.tryDestroy(singleton);
        singleton = null;
        injector = null;
        lookup.clear();
    }

    private static ConcurrentMap<BeanSpec, ProvidedValueLoader> lookup = new ConcurrentHashMap<BeanSpec, ProvidedValueLoader>();

    public static ProvidedValueLoader get(BeanSpec beanSpec, DependencyInjector<?> injector) {
        ProvidedValueLoader loader = lookup.get(beanSpec);
        if (null == loader) {
            loader = new ProvidedValueLoader(beanSpec, injector);
            lookup.putIfAbsent(beanSpec, loader);
        }
        return loader;
    }
}
