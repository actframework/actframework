package act.util;

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
import act.db.AdaptiveRecord;
import act.db.Model;
import act.plugin.AppServicePlugin;
import act.validation.Password;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.Injector;
import org.osgl.util.*;

import java.beans.Transient;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public interface EnhancedAdaptiveMap<T extends EnhancedAdaptiveMap> extends AdaptiveMap<T> {
    /**
     * Returns the meta info of this EnhancedAdaptiveMap
     *
     * @return AdaptiveRecord meta info
     */
    @Transient
    MetaInfo metaInfo();

    class Util {

        public static <MODEL_TYPE extends EnhancedAdaptiveMap> MODEL_TYPE putValue(MODEL_TYPE ar, String key, Object val) {
            Map<String, Object> kv = ar.internalMap();
            $.Func2 setter = ar.metaInfo().fieldSetters.get(key);
            if (null != setter) {
                setter.apply(ar, val);
            } else {
                kv.put(key, val);
            }
            return ar;
        }

        public static <MODEL_TYPE extends EnhancedAdaptiveMap> MODEL_TYPE mergeValue(MODEL_TYPE ar, String key, Object val) {
            Map<String, Object> kv = ar.internalMap();
            $.Func2 merger = ar.metaInfo().fieldMergers.get(key);
            if (null != merger) {
                merger.apply(ar, val);
            } else {
                Object v0 = kv.get(key);
                kv.put(key, MetaInfo.merge(v0, val));
            }
            return ar;
        }

        public static <MODEL_TYPE extends EnhancedAdaptiveMap, T> T getValue(MODEL_TYPE ar, String key) {
            Map<String, Object> kv = ar.internalMap();
            $.Function getter = ar.metaInfo().fieldGetters.get(key);
            if (null != getter) {
                return (T) getter.apply(ar);
            }
            return (T) kv.get(key);
        }

        public static <MODEL_TYPE extends EnhancedAdaptiveMap> MODEL_TYPE putValues(MODEL_TYPE ar, Map<String, Object> map) {
            MetaInfo metaInfo = generateMetaInfo(ar);
            Map<String, BeanInfo> beanSpecMap = metaInfo.setterFieldSpecs;
            final boolean isModel = ar instanceof Model;
            for (Map.Entry<String, Object> entry: map.entrySet()) {
                String key = entry.getKey();
                if ("id".equals(key)) {
                    continue;
                }
                Object val = entry.getValue();
                BeanInfo spec = beanSpecMap.get(key);
                if (isModel && null != spec && spec.hasAnnotation(Password.class)) {
                    if (null == val || S.blank($.convert(val).toString())) {
                        if (((Model)ar)._isNew()) {
                            // generating random password for new record
                            val = S.secureRandom(11);
                        } else {
                            // skip empty password for record updating
                            continue;
                        }
                    }
                }
                ar.putValue(key, val);
            }
            return ar;
        }

        public static <MODEL_TYPE extends EnhancedAdaptiveMap> MODEL_TYPE mergeValues(MODEL_TYPE ar, Map<String, Object> map) {
            MetaInfo metaInfo = generateMetaInfo(ar);
            Map<String, BeanInfo> beanSpecMap = metaInfo.setterFieldSpecs;
            final boolean isModel = ar instanceof Model;
            for (Map.Entry<String, Object> entry: map.entrySet()) {
                String key = entry.getKey();
                if ("id".equals(key)) {
                    continue;
                }
                Object val = entry.getValue();
                BeanInfo spec = beanSpecMap.get(key);
                if (null != spec && spec.hasAnnotation(Password.class)) {
                    if (null == val || S.blank($.convert(val).toString())) {
                        if (((Model)ar)._isNew()) {
                            // generating random password for new record
                            val = S.secureRandom(11);
                        } else {
                            // skip empty password for record updating
                            continue;
                        }
                    }
                }
                ar.mergeValue(key, val);
            }
            return ar;
        }

        public static <MODEL_TYPE extends EnhancedAdaptiveMap, T> boolean containsKey(MODEL_TYPE ar, String key) {
            Map<String, Object> kv = ar.internalMap();
            return kv.containsKey(key) || ar.metaInfo().getterFieldSpecs.containsKey(key);
        }

        public static Map<String, Object> toMap(final EnhancedAdaptiveMap ar) {
            Map<String, Object> kv = ar.internalMap();
            Map<String, Object> map = new LinkedHashMap<>(kv);
            for (Map.Entry<String, $.Function> entry : ar.metaInfo().fieldGetters.entrySet()) {
                map.put(entry.getKey(), entry.getValue().apply(ar));
            }
            return map;
        }

        private static int fieldsSize(final EnhancedAdaptiveMap ar) {
            return ar.metaInfo().getterFieldSpecs.size();
        }

        public static int size(final EnhancedAdaptiveMap ar) {
            Map<String, Object> kv = ar.internalMap();
            return kv.size() + fieldsSize(ar);
        }

        private static boolean hasFields(EnhancedAdaptiveMap ar) {
            return !ar.metaInfo().getterFieldSpecs.isEmpty();
        }


        public static Set<String> keySet(EnhancedAdaptiveMap ar) {
            Map<String, Object> kv = ar.internalMap();
            if (!hasFields(ar)) {
                return kv.keySet();
            }
            Set<String> set = new HashSet<String>(ar.metaInfo().getterFieldSpecs.keySet());
            set.addAll(kv.keySet());
            return set;
        }

        public static Set<Map.Entry<String, Object>> entrySet(EnhancedAdaptiveMap ar, $.Function<BeanInfo, Boolean> function) {
            Map<String, Object> kv = ar.internalMap();
            if (!hasFields(ar)) {
                return kv.entrySet();
            }
            Set<Map.Entry<String, Object>> set = new LinkedHashSet<Map.Entry<String, Object>>(kv.entrySet());
            MetaInfo metaInfo = ar.metaInfo();
            boolean filter = null != function;
            for (Map.Entry<String, $.Function> entry: metaInfo.fieldGetters.entrySet()) {
                String fieldName = entry.getKey();
                if ("kv".equals(fieldName)) {
                    continue;
                }
                if (filter) {
                    BeanInfo field = metaInfo.getterFieldSpecs.get(fieldName);
                    if (!function.apply(field)) {
                        continue;
                    }
                }
                $.Function getter = entry.getValue();
                set.add(new C.Map.Entry(fieldName, getter.apply(ar)));
            }
            return set;
        }


        public static Map<String, Object> asMap(final EnhancedAdaptiveMap ar) {
            final Map<String, Object> kv = ar.internalMap();
            // TODO: should we check the field value on size, remove, containsXxx etc methods?
            return new AbstractMap<String, Object>() {

                @Override
                public int size() {
                    return ar.size();
                }

                @Override
                public boolean isEmpty() {
                    return ar.size() == 0;
                }

                @Override
                public boolean containsKey(Object key) {
                    return kv.containsKey(key) || ar.metaInfo().getterFieldSpecs.containsKey(key);
                }

                @Override
                public boolean containsValue(Object value) {
                    return kv.containsValue(value);
                }

                @Override
                public Object get(Object key) {
                    $.Function getter = ar.metaInfo().fieldGetters.get(key);
                    return null != getter ? getter.apply(ar) : kv.get((String)key);
                }

                @Override
                public Object put(String key, Object value) {
                    $.Func2 setter = ar.metaInfo().fieldSetters.get(key);
                    if (null != setter) {
                        Object o = get(key);
                        setter.apply(ar, value);
                        return o;
                    }
                    return kv.put(key, value);
                }

                @Override
                public Object remove(Object key) {
                    $.Function getter = ar.metaInfo().fieldGetters.get(key);
                    if (null != getter) {
                        return null;
                    } else {
                        return kv.remove(key);
                    }
                }

                @Override
                public void putAll(Map<? extends String, ?> m) {
                    ar.putValues((Map)m);
                }

                @Override
                public void clear() {
                    kv.clear();
                    // TODO: should we clear field values?
                }

                @Override
                public Set<String> keySet() {
                    return ar.keySet();
                }

                @Override
                public Collection<Object> values() {
                    List<Object> list = new ArrayList<Object>();
                    list.addAll(kv.values());
                    for ($.Function getter : ar.metaInfo().fieldGetters.values()) {
                        list.add(getter.apply(ar));
                    }
                    return list;
                }

                @Override
                public Set<Entry<String, Object>> entrySet() {
                    return ar.entrySet();
                }
            };
        }

        public static MetaInfo generateMetaInfo(EnhancedAdaptiveMap ar) {
            MetaInfo.Repository r = Act.appServicePluginManager().get(MetaInfo.Repository.class);
            return r.get(ar.getClass(), new $.Transformer<Class<? extends EnhancedAdaptiveMap>, MetaInfo>() {
                @Override
                public MetaInfo transform(Class<? extends EnhancedAdaptiveMap> aClass) {
                    return new MetaInfo(aClass);
                }
            });
        }
    }

    class MetaInfo {
        private Class<? extends EnhancedAdaptiveMap> arClass;
        public String className;
        public Map<String, BeanInfo> getterFieldSpecs;
        public Map<String, Class> getterFieldClasses;
        public Map<String, BeanInfo> setterFieldSpecs;
        public Map<String, Class> setterFieldClasses;
        public Map<String, $.Function> fieldGetters;
        public Map<String, $.Func2> fieldSetters;
        public Map<String, $.Func2> fieldMergers;
        private SimpleBean.MetaInfo metaInfo;

        public MetaInfo(Class<? extends EnhancedAdaptiveMap> clazz) {
            this.className = clazz.getName();
            this.metaInfo = Act.app().classLoader().simpleBeanInfoManager().get(className);
            this.arClass = clazz;
            this.discoverProperties(clazz);
            this.orderIndexByFields();
        }

        private void orderIndexByFields() {
            List<Field> fields = $.fieldsOf(arClass);
            if (fields.isEmpty()) {
                return;
            }
            List<String> names = new ArrayList<>(fields.size());
            for (Field field : fields) {
                names.add(field.getName());
            }
            orderIndex(names, getterFieldSpecs);
            orderIndex(names, getterFieldClasses);
            orderIndex(names, setterFieldSpecs);
            orderIndex(names, setterFieldClasses);
            orderIndex(names, fieldGetters);
            orderIndex(names, fieldMergers);
        }

        // pre-assumption index map is LinkedHashMap
        private void orderIndex(List<String> order, Map index) {
            LinkedHashMap bak = new LinkedHashMap<>(index);
            index.clear();
            for (String s : order) {
                Object val = bak.remove(s);
                if (null != val) {
                    index.put(s, val);
                }
            }
            index.putAll(bak);
        }

        @Deprecated
        public Class fieldClass(String fieldName) {
            Class clazz = setterFieldClasses.get(fieldName);
            return null == clazz ? getterFieldClasses.get(fieldName) : clazz;
        }

        public Class getterFieldClass(String fieldName) {
            return getterFieldClasses.get(fieldName);
        }

        public Class setterFieldClass(String fieldName) {
            return setterFieldClasses.get(fieldName);
        }

        public Type getterFieldType(String fieldName) {
            BeanInfo spec = getterFieldSpecs.get(fieldName);
            return null == spec ? null : spec.type();
        }

        public Type setterFieldType(String fieldName) {
            BeanInfo spec = setterFieldSpecs.get(fieldName);
            return null == spec ? null : spec.type();
        }

        private void discoverProperties(Class<? extends EnhancedAdaptiveMap> clazz) {
            getterFieldSpecs = new LinkedHashMap<>();
            getterFieldClasses = new LinkedHashMap<>();
            setterFieldSpecs = new LinkedHashMap<>();
            setterFieldClasses = new LinkedHashMap<>();
            fieldGetters = new LinkedHashMap<>();
            fieldSetters = new LinkedHashMap<>();
            fieldMergers = new LinkedHashMap<>();
            Injector injector = Act.app().injector();
            for (final Method m : clazz.getMethods()) {
                String name = propertyName(m);
                String alias, label;
                final boolean hasAlias, hasLabel;
                if (S.blank(name)) {
                    continue;
                } else {
                    name = S.lowerFirst(name);
                    if ("idAsStr".equals(name)) {
                        // special case for MorphiaModel
                        continue;
                    }
                    alias = null == metaInfo ? name : metaInfo.aliasOf(name);
                    hasAlias = S.neq(name, alias);
                    label = null == metaInfo ? name : metaInfo.labelOf(name);
                    hasLabel = S.neq(name, label);
                }
                final Class returnClass = Generics.getReturnType(m, clazz);
                Type returnType = m.getGenericReturnType();
                Class paramClass = null;
                Type paramType = null;
                Class[] params = m.getParameterTypes();
                Type[] paramTypes = m.getGenericParameterTypes();
                if (null != params && params.length == 1) {
                    paramClass = params[0];
                    paramType = paramTypes[0];
                }
                Class fieldClass = null == paramClass ? returnClass : paramClass;
                Type fieldType = null == paramType ? returnType : paramType;
                if (!(fieldType instanceof ParameterizedType)) {
                    fieldType = fieldClass;
                }
                if (null == paramClass) {
                    BeanSpec spec = BeanSpec.of(fieldType, m.getDeclaredAnnotations(), name, injector);
                    getterFieldSpecs.put(name, spec);
                    getterFieldClasses.put(name, fieldClass);
                    if (hasAlias) {
                        getterFieldSpecs.put(alias, spec);
                        getterFieldClasses.put(alias, fieldClass);
                    }
                    if (hasLabel) {
                        getterFieldSpecs.put(label, spec);
                        getterFieldClasses.put(label, fieldClass);
                    }
                } else {
                    BeanInfo existingSpec = setterFieldSpecs.get(name);
                    if (null == existingSpec && hasAlias) {
                        existingSpec = setterFieldSpecs.get(alias);
                    }
                    if (null == existingSpec && hasLabel) {
                        existingSpec = setterFieldSpecs.get(label);
                    }
                    if (null != existingSpec) {
                        // we need to infer the type from field in this case
                        Field field = $.fieldOf(clazz, name, true);
                        if (null != field) {
                            BeanSpec spec = BeanSpec.of(field, injector);
                            setterFieldSpecs.put(name, spec);
                            setterFieldClasses.put(name, field.getType());
                            if (hasAlias) {
                                setterFieldSpecs.put(alias, spec);
                                setterFieldClasses.put(alias, field.getType());
                            }
                            if (hasLabel) {
                                setterFieldSpecs.put(label, spec);
                                setterFieldClasses.put(label, field.getType());
                            }
                        } else {
                            if (fieldClass == Object.class) {
                                // ignore
                            } else if (existingSpec.rawType() == Object.class) {
                                BeanSpec spec = BeanSpec.of(fieldType, m.getDeclaredAnnotations(), name, injector);
                                setterFieldSpecs.put(name, spec);
                                setterFieldClasses.put(name, fieldClass);
                                if (hasAlias) {
                                    setterFieldSpecs.put(alias, spec);
                                    setterFieldClasses.put(alias, fieldClass);
                                }
                                if (hasLabel) {
                                    setterFieldSpecs.put(label, spec);
                                    setterFieldClasses.put(label, field.getType());
                                }
                            }
                        }
                    } else {
                        Annotation[] annotations = m.getAnnotations();
                        Field field = $.fieldOf(clazz, name);
                        if (null != field) {
                            Annotation[] fieldAnnotations = field.getAnnotations();
                            if (annotations.length == 0) {
                                annotations = fieldAnnotations;
                            } else if (fieldAnnotations.length > 0) {
                                annotations = $.concat(annotations, fieldAnnotations);
                            }
                        }
                        BeanSpec spec = BeanSpec.of(fieldType, annotations, name, injector);
                        setterFieldSpecs.put(name, spec);
                        setterFieldClasses.put(name, fieldClass);
                        if (hasAlias) {
                            setterFieldSpecs.put(alias, spec);
                            setterFieldClasses.put(alias, fieldClass);
                        }
                        if (hasLabel) {
                            setterFieldSpecs.put(label, spec);
                            setterFieldClasses.put(label, field.getType());
                        }
                    }
                }
                if (null != paramClass) {
                    final String fieldName = name;
                    $.Func2 fn = new $.Func2() {
                        @Override
                        public Object apply(Object host, Object value) throws NotAppliedException, $.Break {
                            BeanInfo spec = setterFieldSpecs.get(fieldName);
                            if (null != value && !spec.isInstance(value)) {
                                if (value instanceof String) {
                                    value = Act.app().resolverManager().resolve((String)value, spec.rawType());
                                } else if (value instanceof JSONObject) {
                                    value = JSON.parseObject(((JSONObject) value).toJSONString(), spec.rawType());
                                }
                            }
                            $.invokeVirtual(host, m, value);
                            return null;
                        }
                    };
                    fieldSetters.put(name, fn);
                    if (hasAlias) {
                        fieldSetters.put(alias, fn);
                    }
                    if (hasLabel) {
                        fieldSetters.put(label, fn);
                    }
                    fn = new $.Func2() {
                        @Override
                        public Object apply(Object host, Object value) throws NotAppliedException, $.Break {
                            BeanInfo spec = setterFieldSpecs.get(fieldName);
                            if (null != value && !spec.isInstance(value)) {
                                if (value instanceof String) {
                                    value = Act.app().resolverManager().resolve((String)value, spec.rawType());
                                }
                            }
                            $.Function getter = fieldGetters.get(fieldName);
                            if (null == getter) {
                                $.invokeVirtual(host, m, value);
                                return null;
                            }
                            Object value0 = getter.apply(host);
                            value = merge(value0, value);
                            $.invokeVirtual(host, m, value);
                            return null;
                        }
                    };
                    fieldMergers.put(name, fn);
                    if (hasAlias) {
                        fieldMergers.put(alias, fn);
                    }
                    if (hasLabel) {
                        fieldMergers.put(label, fn);
                    }
                } else {
                    $.F1 fn = new $.F1() {
                        @Override
                        public Object apply(Object host) throws NotAppliedException, $.Break {
                            return $.invokeVirtual(host, m);
                        }
                    };
                    fieldGetters.put(name, fn);
                    if (hasAlias) {
                        fieldGetters.put(alias, fn);
                    }
                    if (hasLabel) {
                        fieldGetters.put(label, fn);
                    }
                }
            }
        }

        private String propertyName(Method m) {
            String name = m.getName();
            if ("getClass".equals(name)) {
                return null;
            }
            Type[] paramTypes = m.getGenericParameterTypes();
            if (name.startsWith("set") && void.class == m.getReturnType() && null != paramTypes && paramTypes.length == 1) {
                return name.substring(3);
            }
            boolean isGet = name.startsWith("get");
            boolean isIs = name.startsWith("is");
            if ((isGet || isIs) && void.class != m.getReturnType() && (null == paramTypes || paramTypes.length == 0)) {
                return isGet ? name.substring(3) : name.substring(2);
            }
            return null;
        }

        public static Object merge(Object to, Object from) {
            if (null == to) {
                return from;
            }
            if (null == from) {
                return to;
            }
            if (canBeMerged(to.getClass())) {
                return _merge(to, from);
            }
            return from;
        }

        private static Object _merge(Object to, Object from) {
            if (to instanceof ValueObject) {
                if (from instanceof ValueObject) {
                    return ValueObject.of(merge(((ValueObject) to).value(), ((ValueObject) from).value()));
                }
                return ValueObject.of(merge(((ValueObject) to).value(), from));
            }
            if (to instanceof AdaptiveRecord) {
                AdaptiveRecord ar = (AdaptiveRecord) to;
                return mergeIntoAdaptiveRecord(ar, from);
            }
            if (to instanceof Map) {
                Map map = (Map) to;
                return mergeIntoMap(map, from);
            }
            if (to instanceof Set) {
                Set set = (Set) to;
                return mergeIntoSet(set, from);
            }
            if (to instanceof List) {
                List list = (List) to;
                return mergeIntoList(list, from);
            }
            if (to.getClass().isArray()) {
                List list = new ArrayList();
                int len = Array.getLength(to);
                for (int i = 0; i < len; ++i) {
                    list.add(Array.get(to, i));
                }
                List list1 = mergeIntoList(list, from);
                int sz1 = list1.size();
                Object a1 = Array.newInstance(to.getClass().getComponentType(), sz1);
                for (int i = 0; i < sz1; ++i) {
                    Array.set(a1, i, list1.get(i));
                }
                return a1;
            }
            return mergeIntoPojo(to, from);
        }

        private static String getterName(Field field) {
            boolean isBoolean = field.getType() == Boolean.class || field.getType() == boolean.class;
            return (isBoolean ? "is" : "get") + S.capFirst(field.getName());
        }

        private static String setterName(Field field) {
            return "set" + S.capFirst(field.getName());
        }

        private static boolean canBeMerged(Class<?> c) {
            return !($.isSimpleType(c) || isDateType(c));
        }

        private static boolean isDateType(Class<?> c) {
            String name = c.getSimpleName();
            return (name.endsWith("Date") || name.endsWith("DateTime") || name.endsWith("Calendar"));
        }

        private static AdaptiveRecord mergeIntoAdaptiveRecord(AdaptiveRecord ar, Object value) {
            if (value instanceof Map) {
                return mergeMapIntoAdaptiveRecord(ar, (Map) value);
            }
            if (value instanceof AdaptiveRecord) {
                return mergeMapIntoAdaptiveRecord(ar, ((AdaptiveRecord) value).asMap());
            }
            List<Field> fields = $.fieldsOf(value.getClass(), true);
            for (Field f : fields) {
                f.setAccessible(true);
                try {
                    String fn = f.getName();
                    Object fv = f.get(value);
                    Object o0 = ar.getValue(fn);
                    ar.putValue(fn, merge(o0, fv));
                } catch (IllegalAccessException e) {
                    throw E.unexpected(e, "error merging into adaptive record");
                }
            }
            return ar;
        }

        private static AdaptiveRecord mergeMapIntoAdaptiveRecord(AdaptiveRecord ar, Map map) {
            return ar.putValues(map);
        }

        private static Map mergeIntoMap(Map map, Object value) {
            if (value instanceof Map) {
                return mergeMapIntoMap(map, (Map) value);
            }
            if (value instanceof AdaptiveRecord) {
                return mergeMapIntoMap(map, ((AdaptiveRecord) value).asMap());
            }
            Map retval = new LinkedHashMap(map);
            List<Field> fields = $.fieldsOf(value.getClass(), true);
            for (Field f : fields) {
                f.setAccessible(true);
                try {
                    String fn = f.getName();
                    Object fv = f.get(value);
                    Object o0 = map.get(fn);
                    retval.put(fn, merge(o0, fv));
                } catch (IllegalAccessException e) {
                    throw E.unexpected(e, "error merging into adaptive record");
                }
            }
            return retval;
        }

        private static Map mergeMapIntoMap(Map m0, Map<?, ?> m1) {
            Map retval = (Map) Act.injector().get(m0.getClass());
            retval.putAll(m0);
            for (Map.Entry entry : m1.entrySet()) {
                Object k = entry.getKey();
                Object v = entry.getValue();
                retval.put(k, merge(m0.get(k), v));
            }
            return retval;
        }

        private static Set mergeIntoSet(Set set, Object value) {
            if (value instanceof Collection) {
                return mergeCollectionIntoSet(set, (Collection) value);
            }
            if (value.getClass().isArray()) {
                List list = new ArrayList();
                int len = Array.getLength(value);
                for (int i = 0; i < len; ++i) {
                    list.add(Array.get(value, i));
                }
                return mergeCollectionIntoSet(set, list);
            }
            throw new IllegalArgumentException("Cannot merge " + value.getClass() + " into Set");
        }

        private static Set mergeCollectionIntoSet(Set set, Collection col) {
            Set set1 = (Set) Act.injector().get(set.getClass());
            set1.addAll(col);
            return set1;
        }

        private static List mergeIntoList(List list, Object value) {
            if (value instanceof Set) {
                return mergeSetIntoList(list, (Set) value);
            }
            if (value instanceof List) {
                return mergeListIntoList(list, (List) value);
            }
            if (value.getClass().isArray()) {
                List list0 = new ArrayList();
                int len = Array.getLength(value);
                for (int i = 0; i < len; ++i) {
                    list0.add(Array.get(value, i));
                }
                return mergeListIntoList(list, list0);
            }
            throw new IllegalArgumentException("Cannot merge " + value.getClass() + " into List");
        }

        private static List mergeSetIntoList(List list, Set set) {
            List retval = (List) Act.injector().get(list.getClass());
            retval.addAll(list);
            retval.addAll(set);
            return retval;
        }

        private static List mergeListIntoList(List to, List from) {
            List retval = (List) Act.injector().get(to.getClass());
            int szTo = to.size();
            int szFrom = from.size();
            int szMin = Math.min(szTo, szFrom);
            for (int i = 0; i < szMin; ++i) {
                Object o0 = to.get(i);
                Object o1 = from.get(i);
                retval.add(merge(o0, o1));
            }
            if (szTo > szFrom) {
                for (int i = szMin; i < szTo; ++i) {
                    retval.add(to.get(i));
                }
            } else if (szFrom > szTo) {
                for (int i = szMin; i < szFrom; ++i) {
                    retval.add(from.get(i));
                }
            }
            return retval;
        }

        private static Object mergeIntoPojo(Object o0, Object o1) {
            if (o1 instanceof Map) {
                return mergeMapIntoPojo(o0, (Map) o1);
            }
            if (o1 instanceof AdaptiveRecord) {
                return mergeMapIntoPojo(o0, ((AdaptiveRecord) o1).asMap());
            }
            if (o1 instanceof Collection || o1.getClass().isArray()) {
                throw E.unexpected("cannot merge " + o1.getClass() + " into " + o0.getClass());
            }
            Class<?> c0 = o0.getClass();
            List<Field> fields = $.fieldsOf(o1.getClass(), true);
            for (Field f : fields) {
                f.setAccessible(true);
                try {
                    String fn = f.getName();
                    Field f0 = $.fieldOf(c0, fn);
                    if (null == f0) {
                        continue;
                    }
                    Object fv = f.get(o1);
                    f0.setAccessible(true);
                    Object fv0 = merge(f0.get(o0), fv);
                    f0.set(o0, fv0);
                } catch (IllegalAccessException e) {
                    throw E.unexpected(e, "error merging into POJO");
                }
            }
            return o0;
        }

        private static Object mergeMapIntoPojo(Object o0, Map map) {
            List<Field> fields = $.fieldsOf(o0.getClass(), true);
            for (Field f : fields) {
                String fn = f.getName();
                if (map.containsKey(fn)) {
                    f.setAccessible(true);
                    try {
                        Object v = f.get(o0);
                        f.set(o0, merge(v, map.get(fn)));
                    } catch (IllegalAccessException e) {
                        throw E.unexpected(e, "error merging into POJO");
                    }
                }
            }
            return o0;
        }

        public static class Repository extends AppServicePlugin {
            @Override
            protected void applyTo(App app) {
            }

            private ConcurrentMap<Class<?>, MetaInfo> map = new ConcurrentHashMap<>();

            public MetaInfo get(Class<? extends EnhancedAdaptiveMap> clazz, $.Function<Class<? extends EnhancedAdaptiveMap>, MetaInfo> factory) {
                MetaInfo info = map.get(clazz);
                if (null == info) {
                    MetaInfo theInfo = factory.apply(clazz);
                    info = map.putIfAbsent(clazz, theInfo);
                    if (null == info) {
                        info = theInfo;
                    }
                }
                return info;
            }
        }
    }

}
