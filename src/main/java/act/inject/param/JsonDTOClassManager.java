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

import act.app.App;
import act.app.AppClassLoader;
import act.app.AppServiceBase;
import act.db.DbBind;
import act.inject.DependencyInjector;
import org.osgl.$;
import org.osgl.exception.UnexpectedException;
import org.osgl.inject.BeanSpec;
import org.osgl.util.E;
import org.osgl.util.Generics;
import org.osgl.util.S;

import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class JsonDTOClassManager extends AppServiceBase<JsonDTOClassManager> {

    public static final String CTX_ATTR_KEY = "__json_dto__";

    static class DynamicClassLoader extends ClassLoader {
        private DynamicClassLoader(AppClassLoader parent) {
            super(parent);
        }

        Class<?> defineClass(String name, byte[] b) {
            AppClassLoader loader = (AppClassLoader) getParent();
            return loader.defineClass(name, b, 0, b.length, true);
        }
    }

    private ConcurrentMap<String, Class<? extends JsonDTO>> dtoClasses = new ConcurrentHashMap<String, Class<? extends JsonDTO>>();

    private DependencyInjector<?> injector;
    private DynamicClassLoader dynamicClassLoader;


    public JsonDTOClassManager(App app) {
        super(app);
        this.injector = app.injector();
        this.dynamicClassLoader = new DynamicClassLoader(app.classLoader());
    }

    @Override
    protected void releaseResources() {

    }

    public Class<? extends JsonDTO> get(Class<?> host, Method method) {
        List<BeanSpec> beanSpecs = beanSpecs(host, method);
        String key = key(beanSpecs);
        if (S.blank(key)) {
            return null;
        }
        Class<? extends JsonDTO> cls = dtoClasses.get(key);
        if (null == cls) {
            try {
                Class<? extends JsonDTO> newClass = generate(key, beanSpecs);
                cls = dtoClasses.putIfAbsent(key, newClass);
                if (null == cls) {
                    cls = newClass;
                }
            } catch (LinkageError e) {
                if (e.getMessage().contains("duplicate class definition")) {
                    // another thread has already the DTO class
                    cls = dtoClasses.get(key);
                    E.unexpectedIf(null == cls, "We don't know what happened here ...");
                } else {
                    throw e;
                }
            }
        }
        return cls;
    }

    private Class<? extends JsonDTO> generate(String name, List<BeanSpec> beanSpecs) {
        return new JsonDTOClassGenerator(name, beanSpecs, dynamicClassLoader).generate();
    }

    public static final $.Predicate<Class<?>> CLASS_FILTER = new $.Predicate<Class<?>>() {
        @Override
        public boolean test(Class<?> aClass) {
            if (null == aClass || Object.class == aClass) {
                return false;
            }
            Annotation[] annotations = aClass.getDeclaredAnnotations();
            if (null == annotations) {
                return true;
            }
            for (Annotation a : annotations) {
                if (a.annotationType() == NoBind.class) {
                    return false;
                }
            }
            return true;
        }
    };

    public static final $.Predicate<Field> FIELD_FILTER = new $.Predicate<Field>() {
        @Override
        public boolean test(Field field) {
            return !Modifier.isStatic(field.getModifiers());
        }
    };

    public List<BeanSpec> beanSpecs(Class<?> host, Method method) {
        List<BeanSpec> list = new ArrayList<BeanSpec>();
        if (!Modifier.isStatic(method.getModifiers())) {
            extractBeanSpec(list, $.fieldsOf(host, CLASS_FILTER, FIELD_FILTER), host);
        }
        extractBeanSpec(list, method, host);
        Collections.sort(list, CMP);
        return list;
    }

    private void extractBeanSpec(List<BeanSpec> beanSpecs, List<Field> fields, Class<?> host) {
        for (Field field : fields) {
            BeanSpec spec = null;
            Type genericType = field.getGenericType();
            if (genericType instanceof Class || genericType instanceof ParameterizedType) {
                spec = BeanSpec.of(field.getGenericType(), field.getDeclaredAnnotations(), field.getName(), injector);
            } else if (genericType instanceof TypeVariable) {
                // can determine type by field, check inject constructor parameter
                TypeVariable tv = (TypeVariable)genericType;
                Type[] bounds = tv.getBounds();
                if (bounds != null && bounds.length == 1) {
                    Type bound = bounds[0];
                    if (bound instanceof ParameterizedType || bound instanceof Class) {
                        Class<?> boundClass = BeanSpec.rawTypeOf(bound);
                        Constructor<?>[] ca = host.getConstructors();
                        CONSTRUCTORS:
                        for (Constructor<?> c : ca) {
                            if (c.getAnnotation(Inject.class) != null) {
                                // check all param types
                                Type[] constructorParams = c.getGenericParameterTypes();
                                for (Type paramType : constructorParams) {
                                    if (paramType instanceof ParameterizedType || paramType instanceof Class) {
                                        Class<?> paramClass = BeanSpec.rawTypeOf(paramType);
                                        if (boundClass.isAssignableFrom(paramClass)) {
                                            spec = BeanSpec.of(paramType, field.getDeclaredAnnotations(), field.getName(), injector);
                                            break CONSTRUCTORS;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (null == spec) {
                throw E.unexpected("Cannot determine bean spec of field: %s", field);
            }
            if (ParamValueLoaderService.noBindOrProvided(spec, injector)) {
                return;
            }
            String dbBindName = dbBindName(spec);
            if (null != dbBindName) {
                beanSpecs.add(BeanSpec.of(String.class, new Annotation[0], dbBindName, injector));
            } else {
                beanSpecs.add(spec);
            }
        }
    }

    private void extractBeanSpec(List<BeanSpec> beanSpecs, Method method, Class host) {
        Type[] paramTypes = method.getGenericParameterTypes();
        int sz = paramTypes.length;
        if (0 == sz) {
            return;
        }
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < sz; ++i) {
            Type type = paramTypes[i];
            if (type instanceof TypeVariable && !Modifier.isStatic(method.getModifiers())) {
                // explore type variable impl
                TypeVariable typeVar = $.cast(type);
                String typeVarName = typeVar.getName();
                // find all generic types on host
                Map<String, Class> typeVarLookup = Generics.buildTypeParamImplLookup(host);
                type = typeVarLookup.get(typeVarName);
                if (null == type) {
                    throw new UnexpectedException("Cannot determine concrete type of method parameter %s", typeVarName);
                }
            }
            Annotation[] anno = annotations[i];
            BeanSpec spec = BeanSpec.of(type, anno, injector);
            if (ParamValueLoaderService.noBindOrProvided(spec, injector)) {
                return;
            }
            String dbBindName = dbBindName(spec);
            if (null != dbBindName) {
                beanSpecs.add(BeanSpec.of(String.class, new Annotation[0], dbBindName, injector));
            } else {
                beanSpecs.add(spec);
            }
        }
    }

    private static String dbBindName(BeanSpec spec) {
        for (Annotation annotation : spec.allAnnotations()) {
            if (annotation.annotationType().getName().equals(DbBind.class.getName())) {
                String value = $.invokeVirtual(annotation, "value");
                return (S.blank(value)) ? spec.name() : value;
            }
        }
        return null;
    }

    private static final Comparator<BeanSpec> CMP = new Comparator<BeanSpec>() {
        @Override
        public int compare(BeanSpec o1, BeanSpec o2) {
            return o1.name().compareTo(o2.name());
        }
    };

    private static String key(List<BeanSpec> beanSpecs) {
        S.Buffer sb = S.buffer();
        for (BeanSpec beanSpec : beanSpecs) {
            sb.append(beanSpec.name()).append(beanSpec.type().hashCode());
        }
        return sb.toString();
    }

}
