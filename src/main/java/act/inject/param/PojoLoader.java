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

import act.inject.genie.GenieInjector;
import act.util.ActContext;
import act.util.LogSupport;
import org.osgl.$;
import org.osgl.inject.InjectException;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static act.inject.param.ParamValueLoaderService.shouldWaive;

class PojoLoader extends LogSupport implements ParamValueLoader {

    final ParamKey key;
    final Class type;
    final GenieInjector injector;
    final ParamValueLoaderService service;
    protected Map<String, FieldLoader> fieldLoaders;

    public PojoLoader(ParamKey key, Class type, ParamValueLoaderService service) {
        this.key = $.notNull(key);
        this.type = $.notNull(type);
        this.injector = service.injector;
        this.service = service;
        this.fieldLoaders = fieldLoaders(key, type);
    }

    @Override
    public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
        final $.Var<Object> beanBag = $.var(bean);
        $.Factory<Object> beanSource = new $.Factory<Object>() {
            @Override
            public Object create() {
                Object bean = beanBag.get();
                if (null == bean) {
                    try {
                        bean = injector.get(type);
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new InjectException(e, "cannot instantiate %s", type);
                    }
                }
                beanBag.set(bean);
                return bean;
            }
        };
        for (FieldLoader fl : fieldLoaders.values()) {
            fl.applyTo(beanSource, context);
        }
        return beanBag.get();
    }

    @Override
    public String bindName() {
        return key.toString();
    }

    private Map<String, FieldLoader> fieldLoaders(ParamKey key, Class type) {
        Class<?> current = type;
        Map<String, FieldLoader> fieldLoaders = new HashMap<>();
        while (null != current && !current.equals(Object.class)) {
            for (Field field : current.getDeclaredFields()) {
                if (shouldWaive(field)) {
                    continue;
                }
                field.setAccessible(true);
                fieldLoaders.put(field.getName(), service.fieldLoader(key, field));
            }
            current = current.getSuperclass();
        }
        return fieldLoaders;
    }

}
