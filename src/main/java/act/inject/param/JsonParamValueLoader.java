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

import act.app.ActionContext;
import act.app.App;
import act.app.data.StringValueResolverManager;
import act.inject.DependencyInjector;
import act.util.ActContext;
import org.osgl.$;
import org.osgl.inject.BeanSpec;

import javax.inject.Provider;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

class JsonParamValueLoader implements ParamValueLoader {

    private static final Provider NULL_PROVIDER = new Provider() {
        @Override
        public Object get() {
            return null;
        }
    };

    private ParamValueLoader fallBack;
    private BeanSpec spec;
    private Provider defValProvider;
    private boolean isPathVariable;

    JsonParamValueLoader(ParamValueLoader fallBack, BeanSpec spec, DependencyInjector<?> injector) {
        this.fallBack = $.notNull(fallBack);
        this.spec = $.notNull(spec);
        this.defValProvider = findDefValProvider(spec, injector);
        ActionContext ctx = ActionContext.current();
        if (null != ctx) {
            isPathVariable = ctx.isPathVar(spec.name());
        }
    }

    @Override
    public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
        if (isPathVariable) {
            return fallBack.load(bean, context, noDefaultValue);
        }
        JsonDTO dto = context.attribute(JsonDTO.CTX_ATTR_KEY);
        if (null == dto) {
            return this.fallBack.load(bean, context, noDefaultValue);
        } else {
            Object o = dto.get(spec.name());
            return null != o ? o : defValProvider.get();
        }
    }

    @Override
    public String bindName() {
        return spec.name();
    }

    private static Provider findDefValProvider(BeanSpec beanSpec, DependencyInjector<?> injector) {
        final Class c = beanSpec.rawType();
        final StringValueResolverManager resolver = App.instance().resolverManager();
        if (c.isPrimitive()) {
            return new Provider() {
                @Override
                public Object get() {
                    return resolver.resolve(null, c);
                }
            };
        } else if (Collection.class.isAssignableFrom(c) || Map.class.isAssignableFrom(c)) {
            return injector.getProvider(c);
        } else if (c.isArray()) {
            return new Provider() {
                @Override
                public Object get() {
                    return Array.newInstance(c.getComponentType(), 0);
                }
            };
        } else {
            return NULL_PROVIDER;
        }
    }

}
