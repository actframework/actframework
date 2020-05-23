package act.apidoc;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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
import act.app.App;
import act.inject.param.NoBind;
import act.inject.param.ParamValueLoaderService;
import act.util.Global;
import act.util.Stateless;
import org.osgl.$;
import org.osgl.OsglConfig;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.Injector;
import org.osgl.storage.ISObject;
import org.osgl.storage.impl.SObject;
import org.osgl.util.*;

import javax.inject.Named;
import javax.inject.Provider;
import java.io.File;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.DelayQueue;

/**
 * Namespace
 */
public abstract class SampleData {
    private SampleData() {
    }

    /**
     * Mark on a field specify the {@link SampleDataProvider} that
     * should be used to generate sample data for the field been marked
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface ProvidedBy {
        Class<? extends Provider> value();
    }

    /**
     * Mark on a field specify the list of string that
     * can be randomly choosen as sample data for the field
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface StringList {
        String[] value();
    }

    /**
     * Mark on a field specify the list of integer that
     * can be randomly choosen as sample data for the field
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface IntList {
        int[] value();
    }

    /**
     * Mark on a field specify the list of double that
     * can be randomly choosen as sample data for the field
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface DoubleList {
        double[] value();
    }

    /**
     * Mark on an implementation class of {@link SampleDataProvider}
     * to specify the sample data category the provider applied
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER})
    public @interface Category {
        SampleDataCategory value();
    }

    /**
     * Mark on an implementation class of {@link SampleDataProvider}
     * to specify the sample data locale the provider applied
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER})
    public @interface Locale {
        String value();
    }

    /**
     * Generate sample data for class `type`.
     * <p>
     * Note do not use this method to generate collection or map type. Instead use
     * * {@link #generateList(Class, String)} for List type data
     * * {@link #generateSet(Class, String)} for Set type data
     * * {@link #generateMap(Class, Class, String)} for Map type data
     *
     * @param type the class of the sample data to be generated
     * @param <T>  the generic type of the class
     * @return the sample data been generated
     */
    public static <T> T generate(Class<T> type) {
        return generate(type, type.getSimpleName());
    }

    public static <T> T generate(Type type) {
        BeanSpec spec = BeanSpec.of(type, Act.injector());
        return generate(spec, (ISampleDataCategory) null);
    }

    public static <T> T generate(Type type, String name) {
        return generate(BeanSpec.of(type, Act.injector()), SampleDataCategoryManager.get(name));
    }

    public static <T> T generate(Type type, ISampleDataCategory category) {
        return generate(BeanSpec.of(type, Act.injector()), category);
    }

    /**
     * Generate sample data for class `type`.
     * <p>
     * Note do not use this method to generate collection or map type. Instead use
     * * {@link #generateList(Class, String)} for List type data
     * * {@link #generateSet(Class, String)} for Set type data
     * * {@link #generateMap(Class, Class, String)} for Map type data
     *
     * @param type the class of the sample data to be generated
     * @param name the name of the data, e.g. username, firstName ...
     * @param <T>  the generic type of the class
     * @return the sample data been generated
     */
    public static <T> T generate(Class<T> type, String name) {
        return generate(type, SampleDataCategoryManager.get(name));
    }

    /**
     * Generate sample data for class `type`.
     * <p>
     * Note do not use this method to generate collection or map type. Instead use
     * * {@link #generateList(Class, String)} for List type data
     * * {@link #generateSet(Class, String)} for Set type data
     * * {@link #generateMap(Class, Class, String)} for Map type data
     *
     * @param type     the class of the sample data to be generated
     * @param category optional, the sample data category to be used
     * @param <T>      the generic type of the class
     * @return the sample data been generated
     */
    public static <T> T generate(Class<T> type, ISampleDataCategory category) {
        return (T) generate(BeanSpec.of(type, Act.injector()), category);
    }

    public static <T> T generate(BeanSpec spec, ISampleDataCategory category) {
        Map<String, Class> typeParamLookup = C.Map();
        Class type = spec.rawType();
        if (type.getGenericSuperclass() instanceof ParameterizedType) {
            typeParamLookup = Generics.buildTypeParamImplLookup(type);
        }
        return generate(spec, category, typeParamLookup, new HashSet<Type>(), new LinkedList<String>());
    }

    private static <T> T generate(
            BeanSpec spec,
            ISampleDataCategory category,
            Map<String, Class> typeParamLookup,
            Set<Type> typeChain,
            Deque<String> nameChain
    ) {
        ProvidedBy providedBy = spec.getAnnotation(ProvidedBy.class);
        if (null != providedBy) {
            return (T) Act.getInstance(providedBy.value()).get();
        }
        try {
            SampleDataProviderManager sampleDataProviderManager = Act.getInstance(SampleDataProviderManager.class);
            Class<T> type = spec.rawType();
            Class sampleDataType = type;
            if (Keyword.class == type) {
                sampleDataType = String.class;
            }
            Object result = sampleDataProviderManager.getSampleData(category, null, sampleDataType);
            if (null != result) {
                return $.convert(result).to(type);
            }
            if (spec.isSimpleType()) {
                return (T) generateRandomSimpleTypedValue(spec);
            } else if (spec.isArray()) {
                BeanSpec elementSpec = spec.componentSpec();
                List list = generateList(elementSpec, category, typeParamLookup, typeChain, nameChain, randCollectionSize());
                Object[] array = (Object[]) Array.newInstance(elementSpec.rawType(), list.size());
                return (T) list.toArray(array);
            } else if (spec.isList()) {
                BeanSpec elementSpec = spec.componentSpec();
                return (T) generateList(elementSpec, category, typeParamLookup, typeChain, nameChain, randCollectionSize());
            } else if (spec.isSet()) {
                BeanSpec elementSpec = spec.componentSpec();
                return (T) generateSet(elementSpec, category, typeParamLookup, typeChain, nameChain, randCollectionSize());
            } else if (spec.isMap()) {
                List<Type> typeParams = spec.typeParams();
                if (typeParams.size() < 2) {
                    return (T) Act.getInstance(spec.rawType());
                }
                BeanSpec keySpec = spec.componentSpec();
                BeanSpec valSpec = BeanSpec.of(typeParams.get(0), Act.injector());
                Map map = (Map) Act.getInstance(spec.rawType());
                return (T) generateMap(keySpec, valSpec, map, typeParamLookup, typeChain, nameChain, randCollectionSize());
            } else if (File.class.isAssignableFrom(spec.rawType())) {
                return (T) new File("/path/to/upload/file");
            } else if (ISObject.class.isAssignableFrom(spec.rawType())) {
                return (T) SObject.of("/path/to/upload/file", "");
            } else {
                return (T) generateSamplePojo(spec, typeParamLookup, typeChain, nameChain);
            }
        } catch (Exception e) {
            Act.LOGGER.warn(e, "Error generating sample data for " + spec);
            return null;
        }
    }

    public static <T> List<T> generateList(Class<T> elementType) {
        ISampleDataCategory category = null;
        return generateList(elementType, category);
    }

    public static <T> List<T> generateList(Class<T> elementType, int sz) {
        ISampleDataCategory category = null;
        return generateList(elementType, category, sz);
    }

    public static <T> List<T> generateList(Class<T> elementType, String name) {
        return generateList(elementType, SampleDataCategoryManager.get(name));
    }

    public static <T> List<T> generateList(Class<T> elementType, String name, int sz) {
        return generateList(elementType, SampleDataCategoryManager.get(name), sz);
    }

    public static <T> List<T> generateList(BeanSpec elementSpec, String name) {
        return generateList(elementSpec, SampleDataCategoryManager.get(name));
    }

    public static <T> List<T> generateList(BeanSpec elementSpec, String name, int sz) {
        return generateList(elementSpec, SampleDataCategoryManager.get(name), sz);
    }

    public static <T> List<T> generateList(Class<T> elementType, ISampleDataCategory category) {
        return generateList(BeanSpec.of(elementType, Act.injector()), category);
    }

    public static <T> List<T> generateList(Class<T> elementType, ISampleDataCategory category, int sz) {
        return generateList(BeanSpec.of(elementType, Act.injector()), category, sz);
    }

    public static <T> List<T> generateList(BeanSpec elementSpec, ISampleDataCategory category) {
        return generateList(elementSpec, category, randCollectionSize());
    }

    public static <T> List<T> generateList(BeanSpec elementSpec, ISampleDataCategory category, int sz) {
        Class elementType = elementSpec.rawType();
        Map<String, Class> typeParamLookup = C.Map();
        if (elementType.getGenericSuperclass() instanceof ParameterizedType) {
            typeParamLookup = Generics.buildTypeParamImplLookup(elementType);
        }
        return generateList(elementSpec, category, typeParamLookup, new HashSet<Type>(), new LinkedList<String>(), sz);
    }

    private static <T> List<T> generateList(
            BeanSpec elementSpec,
            ISampleDataCategory category,
            Map<String, Class> typeParamLookup,
            Set<Type> typeChain,
            Deque<String> nameChain,
            int sz
    ) {
        return generateCollection(elementSpec, new ArrayList<T>(), category, typeParamLookup, typeChain, nameChain, sz);
    }

    public static <T> Set<T> generateSet(Class<T> elementType) {
        return generateSet(elementType, randCollectionSize());
    }

    public static <T> Set<T> generateSet(Class<T> elementType, int sz) {
        ISampleDataCategory category = null;
        return generateSet(elementType, category, sz);
    }

    public static <T> Set<T> generateSet(Class<T> elementType, String name) {
        return generateSet(elementType, name, randCollectionSize());
    }

    public static <T> Set<T> generateSet(Class<T> elementType, String name, int sz) {
        return generateSet(elementType, SampleDataCategoryManager.get(name), sz);
    }

    public static <T> Set<T> generateSet(BeanSpec elementSpec, String name) {
        return generateSet(elementSpec, name, randCollectionSize());
    }

    public static <T> Set<T> generateSet(BeanSpec elementSpec, String name, int sz) {
        return generateSet(elementSpec, SampleDataCategoryManager.get(name), sz);
    }

    public static <T> Set<T> generateSet(Class<T> elementType, ISampleDataCategory category) {
        return generateSet(elementType, category, randCollectionSize());
    }

    public static <T> Set<T> generateSet(Class<T> elementType, ISampleDataCategory category, int sz) {
        return generateSet(BeanSpec.of(elementType, Act.injector()), category, sz);
    }

    public static <T> Set<T> generateSet(BeanSpec elementSpec, ISampleDataCategory category) {
        return generateSet(elementSpec, category, randCollectionSize());
    }
    
    public static <T> Set<T> generateSet(BeanSpec elementSpec, ISampleDataCategory category, int sz) {
        Class elementType = elementSpec.rawType();
        Map<String, Class> typeParamLookup = C.Map();
        if (elementType.getGenericSuperclass() instanceof ParameterizedType) {
            typeParamLookup = Generics.buildTypeParamImplLookup(elementType);
        }
        return generateSet(elementSpec, category, typeParamLookup, new HashSet<Type>(), new LinkedList<String>(), sz);
    }

    private static <T> Set<T> generateSet(
            BeanSpec elementSpec,
            ISampleDataCategory category,
            Map<String, Class> typeParamLookup,
            Set<Type> typeChain,
            Deque<String> nameChain,
            int sz
    ) {
        return generateCollection(elementSpec, new HashSet<T>(), category, typeParamLookup, typeChain, nameChain, sz);
    }

    public static <K, V> Map<K, V> generateMap(Class<K> keyType, Class<V> valType, String name) {
        return generateMap(keyType, valType, SampleDataCategoryManager.get(name));
    }

    public static <K, V> Map<K, V> generateMap(Class<K> keyType, Class<V> valType, ISampleDataCategory category) {
        Map<K, V> map = new HashMap<>();
        int sz = N.randInt(4, 10);
        for (int i = 0; i < sz; ++i) {
            K key = generate(keyType, category);
            V val = generate(valType, category);
            map.put(key, val);
        }
        return map;
    }

    private static <K, V> Map<K, V> generateMap(
            BeanSpec keySpec,
            BeanSpec valSpec,
            Map map,
            Map<String, Class> typeParamLookup,
            Set<Type> typeChain,
            Deque<String> nameChain,
            int sz
    ) {
        if (!nameChain.isEmpty()) {
            String lastName = nameChain.pop();
            nameChain.push(S.singularize(lastName));
        }
        for (int i = 0; i < sz; ++i) {
            K k = generate(keySpec, null, typeParamLookup, typeChain, nameChain);
            V v = generate(valSpec, null, typeParamLookup, typeChain, nameChain);
            map.put(k, v);
        }
        return map;
    }

    private static Object generateRandomSimpleTypedValue(BeanSpec spec) {
        Class<?> classType = spec.rawType();
        if (classType.isEnum()) {
            Class<? extends Enum<?>> ec = $.cast(classType);
            return $.random(ec);
        } else if (String.class == classType) {
            return S.random();
        } else if (int.class == classType) {
            return N.randInt(10000);
        } else if (char.class == classType) {
            return 48 + N.randInt(41);
        } else if (short.class == classType) {
            return N.randInt(256);
        } else if (byte.class == classType) {
            return N.randInt(128);
        } else if (long.class == classType) {
            return N.randLong();
        } else if (float.class == classType) {
            return N.randFloat();
        } else if (double.class == classType) {
            return N.randDouble();
        } else if (boolean.class == classType) {
            return $.random(true, false);
        } else if (java.util.Locale.class == classType) {
            return (Act.appConfig().locale());
        }
        return StringValueResolver.predefined().get(classType).resolve(null);
    }

    private static Object generateSamplePojo(BeanSpec pojoSpec, Map<String, Class> typeParamLookup, Set<Type> typeChain, Deque<String> nameChain) {
        if (typeChain.contains(pojoSpec.type())) {
            return null;
        }
        typeChain.add(pojoSpec.type());
        try {
            Class<?> beanClass = pojoSpec.rawType();
            Object bean;
            try {
                bean = Act.getInstance(beanClass);
            } catch (Exception e) {
                bean = new HashMap<>();
            }
            for (Method m : beanClass.getMethods()) {
                if (BeanSpec.isGetter(m)) {
                    Object retVal = generateSampleReturnData(m, typeParamLookup, typeChain, nameChain);
                    try {
                        $.setProperty(bean, retVal, m.getName().substring(3));
                    } catch (Exception e) {
                        // there is setter but no getter
                        // ignore it
                    }
                }
            }
            for (Field f : beanClass.getFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                Object fieldVal = generateSampleFieldData(f, typeParamLookup, typeChain, nameChain);
                $.setProperty(bean, fieldVal, f.getName());
            }
            return bean;
        } finally {
            typeChain.remove(pojoSpec.type());
        }
    }

    private static Object generateSampleReturnData(
            Method method,
            Map<String, Class> typeParamLookup,
            Set<Type> typeChain,
            Deque<String> nameChain
    ) {
        if (!BeanSpec.isGetter(method)) {
            return null;
        }
        String lastName = nameChain.peekFirst();
        String name = method.getName();
        nameChain.push(name);
        if ("name".equalsIgnoreCase(name)) {
            name = lastName;
        }
        try {
            ISampleDataCategory category = categoryOf(method, name);
            return generate(BeanSpec.of(method.getGenericReturnType(), Act.injector(), typeParamLookup), category, typeParamLookup, typeChain, nameChain);
        } finally {
            nameChain.pop();
        }
    }

    private static Object generateSampleFieldData(
            Field field,
            Map<String, Class> typeParamLookup,
            Set<Type> typeChain,
            Deque<String> nameChain
    ) {
        String lastName = nameChain.peekFirst();
        String name = field.getName();
        nameChain.push(name);
        if ("name".equalsIgnoreCase(name)) {
            name = lastName;
        }
        try {
            ISampleDataCategory category = categoryOf(field, name);
            return generate(BeanSpec.of(field, Act.injector(), typeParamLookup), category, typeParamLookup, typeChain, nameChain);
        } finally {
            nameChain.pop();
        }
    }

    private static <T, C extends Collection<T>> C generateCollection(
            BeanSpec elementSpec,
            C collection,
            ISampleDataCategory category,
            Map<String, Class> typeParamLookup,
            Set<Type> typeChain,
            Deque<String> nameChain,
            int sz
    ) {
        if (!nameChain.isEmpty()) {
            String lastName = nameChain.pop();
            nameChain.push(S.singularize(lastName));
        }
        for (int i = 0; i < sz; ++i) {
            collection.add((T) generate(elementSpec, category, typeParamLookup, typeChain, nameChain));
        }
        return collection;
    }

    private static boolean shouldWaive(Method getter, Class<?> implementClass) {
        int modifiers = getter.getModifiers();
        if (Modifier.isTransient(modifiers) || Modifier.isStatic(modifiers)) {
            return true;
        }
        String fieldName = getter.getName().substring(3);
        Class<?> entityType = Generics.getReturnType(getter, implementClass);
        return ParamValueLoaderService.noBind(entityType)
                || getter.isAnnotationPresent(NoBind.class)
                || getter.isAnnotationPresent(Stateless.class)
                || getter.isAnnotationPresent(Global.class)
                || ParamValueLoaderService.isInBlackList(fieldName)
                || Object.class.equals(entityType)
                || Class.class.equals(entityType)
                || OsglConfig.globalMappingFilter_shouldIgnore(fieldName);
    }

    private static ISampleDataCategory categoryOf(AnnotatedElement annotatedElement, String name) {
        SampleData.Category anno = annotatedElement.getAnnotation(SampleData.Category.class);
        if (null != anno) {
            return anno.value();
        }
        SampleDataCategoryManager categoryManager = Act.getInstance(SampleDataCategoryManager.class);
        Named named = annotatedElement.getAnnotation(Named.class);
        if (null != named) {
            ISampleDataCategory category = categoryManager.getCategory(named.value());
            if (null != category) {
                return category;
            }
        }
        return categoryManager.getCategory(name);
    }
    
    private static int randCollectionSize() {
        return N.randInt(3, 17);
    }

}
