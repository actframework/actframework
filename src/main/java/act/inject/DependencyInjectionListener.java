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

import org.osgl.inject.BeanSpec;

import java.lang.reflect.Field;

/**
 * Listens for injections into instances of type {@link #listenTo()}. Useful for performing further
 * injections, post-injection initialization, and more.
 */
public interface DependencyInjectionListener {

    /**
     * Returns the classes this listener is interested in
     *
     * @return A list of classes
     */
    Class[] listenTo();

    /**
     * Invoked once an instance has been created.
     * <p>
     * If {@link DependencyInjector} inject a field and the {@link Field#getGenericType() generic type} of
     * the field is kind of {@link java.lang.reflect.ParameterizedType}, then the type parameters of that
     * generic type will be passed to the listener
     * </p>
     *
     * @param bean     instance to be returned by {@link DependencyInjector}
     * @param beanSpec the spec about the bean instance
     */
    void onInjection(Object bean, BeanSpec beanSpec);
}
