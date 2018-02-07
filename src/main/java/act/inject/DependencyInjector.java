package act.inject;

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

import act.app.AppService;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.Injector;
import org.osgl.inject.NamedProvider;

import javax.inject.Provider;

public interface DependencyInjector<DI extends DependencyInjector<DI>> extends AppService<DI>, Injector {

    /**
     * Register a {@link DependencyInjectionBinder} to the injector
     * @param binder the binder
     */
    void registerDiBinder(DependencyInjectionBinder binder);

    /**
     * Register a {@link DependencyInjectionListener} to the injector
     * @param listener the dependency injection event listener
     */
    void registerDiListener(DependencyInjectionListener listener);

    /**
     * Register a {@link Provider} to the injector
     * @param type the target type
     * @param provider the provider
     * @param <T> the type parameter of the target
     */
    <T> void registerProvider(Class<? super T> type, Provider<? extends T> provider);

    /**
     * Register a {@link NamedProvider} to the injector
     * @param type the target object type
     * @param provider the provider
     * @param <T> the generic type of the target object
     */
    <T> void registerNamedProvider(Class<? super T> type, NamedProvider<? extends T> provider);

    /**
     * Report if a given type is a provided type (e.g. ActContext, All application services etc, DAO)
     * @param type the type to be checked
     * @return `true` if the type is a provided type or `false` otherwise
     */
    boolean isProvided(Class<?> type);

    /**
     * Once an object has been created and ready for injection, this method will be
     * called to call back to the {@link DependencyInjectionListener listeners} that has been
     * {@link #registerDiListener(DependencyInjectionListener) registered}
     * @param bean the object to be injected
     * @param spec the spec about the bean instance
     */
    void fireInjectedEvent(Object bean, BeanSpec spec);

    /**
     * Get a bean instance by class
     * @param clazz the class of the bean instance to be returned
     * @param <T> the generic type of the bean instance
     * @return the bean instance
     */
    <T> T get(Class<T> clazz);
}
