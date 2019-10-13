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

import act.Act;
import act.app.*;
import act.db.DbBind;
import act.inject.DependencyInjector;
import act.util.ReflectedInvokerHelper;
import org.osgl.$;
import org.osgl.exception.UnexpectedException;
import org.osgl.inject.BeanSpec;
import org.osgl.util.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;

public class JsonDtoClassManager extends AppServiceBase<JsonDtoClassManager> {

    static class DynamicClassLoader extends ClassLoader {
        private DynamicClassLoader(AppClassLoader parent) {
            super(parent);
        }

        Class<?> defineClass(String name, byte[] b) {
            AppClassLoader loader = (AppClassLoader) getParent();
            return loader.defineClass(name, b, 0, b.length, true);
        }
    }

    private ConcurrentMap<String, Class<? extends JsonDto>> dtoClasses = new ConcurrentHashMap<String, Class<? extends JsonDto>>();

    private DependencyInjector<?> injector;
    private DynamicClassLoader dynamicClassLoader;
    private Map<$.T2<Class, Method>, List<BeanSpec>> beanSpecCache = new HashMap<>();


    public JsonDtoClassManager(App app) {
        super(app);
        this.injector = app.injector();
        this.dynamicClassLoader = new DynamicClassLoader(app.classLoader());
    }

    @Override
    protected void releaseResources() {

    }

    public Class<? extends JsonDto> get(List<BeanSpec> beanSpecs, Class<?> host) {
        String key = key(beanSpecs);
        if (S.blank(key)) {
            return null;
        }
        Class<? extends JsonDto> cls = dtoClasses.get(key);
        if (null == cls) {
            try {
                Map<String, Class> typeParamLookup = Generics.buildTypeParamImplLookup(host);
                Class<? extends JsonDto> newClass = generate(key, beanSpecs, typeParamLookup);
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

    private Class<? extends JsonDto> generate(String name, List<BeanSpec> beanSpecs, Map<String, Class> typeParamLookup) {
        return new JsonDtoClassGenerator(name, beanSpecs, dynamicClassLoader, typeParamLookup).generate();
    }

    public static final $.Predicate<Class<?>> CLASS_FILTER = new $.Predicate<Class<?>>() {
        @Override
        public boolean test(Class<?> aClass) {
            if (null == aClass || Object.class == aClass) {
                return false;
            }
            if (aClass.isAnnotationPresent(NoBind.class)) {
                return false;
            }
            return !Act.injector().isProvided(aClass);
        }
    };

    public static final $.Predicate<Field> FIELD_FILTER = new $.Predicate<Field>() {
        @Override
        public boolean test(Field field) {
            int mod = field.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isFinal(mod)) {
                return false;
            }
            if (ReflectedInvokerHelper.isGlobalOrStateless(field)) {
                return false;
            }
            if (field.isAnnotationPresent(NoBind.class)) {
                return false;
            }
            DependencyInjector injector = Act.injector();
            return !ParamValueLoaderService.provided(BeanSpec.of(field, injector), injector);
        }
    };

    public List<BeanSpec> beanSpecs(Class<?> host, Method method) {
        $.T2<Class, Method> key = $.cast($.T2(host, method));
        List<BeanSpec> list = beanSpecCache.get(key);
        if (null == list) {
            list = new ArrayList<>();
            beanSpecCache.put(key, list);
            if (!Modifier.isStatic(method.getModifiers())) {
                extractBeanSpec(list, $.fieldsOf(host, CLASS_FILTER, FIELD_FILTER), host);
            }
            extractBeanSpec(list, method, host);
            Collections.sort(list, CMP);
        }
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
            if (ParamValueLoaderService.providedButNotDbBind(spec, injector)) {
                continue;
            }
            String dbBindName = dbBindName(spec);
            if (null != dbBindName) {
                beanSpecs.add(BeanSpec.of(String.class, new Annotation[0], dbBindName, injector));
            } else {
                if (ParamValueLoaderService.providedButNotDbBind(spec, injector)) {
                    return;
                }
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
        List<BeanSpec> newSpecs = new ArrayList<>();
        Annotation[][] annotations = ReflectedInvokerHelper.requestHandlerMethodParamAnnotations(method);
        for (int i = 0; i < sz; ++i) {
            Type type = paramTypes[i];
            boolean isStatic = Modifier.isStatic(method.getModifiers());
            if (type instanceof TypeVariable && !isStatic) {
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
            BeanSpec spec;
            if (type instanceof ParameterizedType && !isStatic) {
                // find all generic types on host
                Map<String, Class> typeVarLookup = Generics.buildTypeParamImplLookup(host);
                spec = BeanSpec.of(type, anno, injector, typeVarLookup);
            } else {
                spec = BeanSpec.of(type, anno, injector);
            }
            if (ParamValueLoaderService.providedButNotDbBind(spec, injector)) {
                continue;
            }
            String dbBindName = dbBindName(spec);
            if (null != dbBindName) {
                newSpecs.add(BeanSpec.of(String.class, new Annotation[0], dbBindName, injector));
            } else {
                newSpecs.add(spec);
            }
        }
        beanSpecs.addAll(newSpecs);
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
            sb.append(S.underscore(beanSpec.name())).append(beanSpec.type().hashCode());
        }
        return sb.toString();
    }

}
