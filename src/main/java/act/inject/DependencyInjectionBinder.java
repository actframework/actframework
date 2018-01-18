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

import act.app.App;
import act.event.ActEvent;
import act.event.SystemEvent;
import org.osgl.util.E;

import javax.inject.Provider;

/**
 * Used to pass class binding resolution to DI plugin(s)
 */
public abstract class DependencyInjectionBinder<T> extends ActEvent implements Provider<T>, SystemEvent {

    private Class<T> targetClass;

    public DependencyInjectionBinder(Object source, Class<T> targetClass) {
        super(source);
        E.NPE(targetClass);
        this.targetClass = targetClass;
    }

    @Override
    public Class eventType() {
        return DependencyInjectionBinder.class;
    }

    public Class<T> targetClass() {
        return targetClass;
    }

    public abstract T resolve(App app);

    @Override
    public T get() {
        return resolve(App.instance());
    }


}
