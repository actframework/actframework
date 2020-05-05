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

import static act.inject.param.ParamValueLoaderService.shouldWaive;

import act.inject.genie.GenieInjector;
import act.util.ActContext;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.InjectException;
import org.osgl.util.S;

import java.lang.reflect.Field;
import java.util.*;

class PojoLoader extends ParamValueLoader.JsonBodySupported {

    final ParamKey key;
    final BeanSpec spec;
    final GenieInjector injector;
    final ParamValueLoaderService service;
    final boolean provided;
    protected Map<String, FieldLoader> fieldLoaders;

    public PojoLoader(ParamKey key, BeanSpec spec, ParamValueLoaderService service) {
        this.key = $.requireNotNull(key);
        this.spec = spec;
        this.injector = service.injector;
        this.service = service;
        this.provided = service.provided(spec, injector);
        this.fieldLoaders = fieldLoaders(key, spec);
    }

    @Override
    public String toString() {
        return S.concat("pojo loader [", bindName(), "|", spec.rawType().getSimpleName(), "]");
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
                        bean = provided ? injector.get(spec) : $.newInstance(spec.rawType());
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new InjectException(e, "cannot instantiate %s", spec);
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

    private Map<String, FieldLoader> fieldLoaders(ParamKey key, BeanSpec spec) {
        Class<?> current = spec.rawType();
        Map<String, FieldLoader> fieldLoaders = new HashMap<>();
        Set<Class<?>> circularReferenceDetector = circularReferenceCounter.get();
        while (null != current && !current.equals(Object.class)) {
            if (null == circularReferenceDetector) {
                circularReferenceDetector = new HashSet<>();
                circularReferenceCounter.set(circularReferenceDetector);
            }
            for (Field field : current.getDeclaredFields()) {
                Class<?> fieldType = field.getType();
                if (shouldWaive(field) || circularReferenceDetector.contains(fieldType)) {
                    continue;
                }
                circularReferenceDetector.add(fieldType);
                try {
                    field.setAccessible(true);
                    String fieldName = field.getName();
                    BeanSpec fieldSpec = spec.field(fieldName);
                    if (null == fieldSpec) {
                        // circular reference
                        continue;
                    }
                    fieldLoaders.put(fieldName, service.fieldLoader(key, field, spec.field(fieldName)));
                } finally {
                    circularReferenceDetector.remove(fieldType);
                }
            }
            current = current.getSuperclass();
        }
        return fieldLoaders;
    }


    static final ThreadLocal<Set<Class<?>>> circularReferenceCounter = new ThreadLocal<>();

}
