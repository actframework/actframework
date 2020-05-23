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

import act.util.ActContext;
import org.osgl.mvc.util.Binder;
import org.osgl.util.S;

/**
 * Use {@link org.osgl.mvc.util.Binder} to load param value
 */
class BoundValueLoader extends ParamValueLoader.Cacheable {

    private Binder binder;
    private String bindModel;

    BoundValueLoader(Binder binder, String model) {
        this.binder = binder;
        this.bindModel = model;
    }

    @Override
    public String toString() {
        return S.concat("bound value loader[", bindModel, "]");
    }

    @Override
    public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
        return binder.resolve(bean, bindModel, context);
    }

    @Override
    public String bindName() {
        return bindModel;
    }

}
