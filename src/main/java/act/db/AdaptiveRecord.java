package act.db;

import act.Act;
import act.app.App;
import act.plugin.AppServicePlugin;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.exception.NotAppliedException;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The `AdaptiveRecord` interface specifies a special {@link Model} in that
 * the fields/columns could be implicitly defined by database
 */
public interface AdaptiveRecord<ID_TYPE, MODEL_TYPE extends AdaptiveRecord> extends Model<ID_TYPE, MODEL_TYPE> {

    /**
     * Add a key/val pair into the active record
     * @param key the key
     * @param val the value
     * @return the active record instance
     */
    MODEL_TYPE putValue(String key, Object val);

    /**
     * Add all key/val pairs from specified kv map into this active record
     * @param kvMap the key/value pair
     * @return this active record instance
     */
    MODEL_TYPE putValues(Map<String, Object> kvMap);

    /**
     * Get value from the active record by key specified
     * @param key the key
     * @param <T> the generic type of the value
     * @return the value or `null` if not found
     */
    <T> T getValue(String key);

    /**
     * Export the key/val pairs from this active record into a map
     * @return the exported map contains all key/val pairs stored in this active record
     */
    Map<String, Object> toMap();

    /**
     * Get the size of the data stored in the active record
     * @return the active record size
     */
    int size();

    /**
     * Check if the active records has a value associated with key specified
     * @param key the key
     * @return `true` if there is value associated with the key in the record, or `false` otherwise
     */
    boolean containsKey(String key);

    /**
     * Returns a set of keys that has value stored in the active record
     * @return the key set
     */
    Set<String> keySet();

    /**
     * Returns a set of entries stored in the active record
     * @return the entry set
     */
    Set<Map.Entry<String, Object>> entrySet();

    /**
     * Returns a Map typed object backed by this active record
     * @return a Map backed by this active record
     */
    Map<String, Object> asMap();

    /**
     * Returns the meta info of this AdaptiveRecord
     * @return
     */
    MetaInfo metaInfo();

    class MetaInfo {
        private Class<? extends AdaptiveRecord> arClass;
        public String className;
        private Class<? extends Annotation> transientAnnotationType;
        public Set<Field> fields;
        public Map<String, Type> fieldTypes;
        public Map<String, $.Function> fieldGetters;
        public Map<String, $.Func2> fieldSetters;

        public MetaInfo(Class<? extends AdaptiveRecord> clazz, Class<? extends Annotation> transientAnnotationType) {
            this.className = clazz.getName();
            this.arClass = clazz;
            this.transientAnnotationType = transientAnnotationType;
            this.discoverFields(clazz);
        }

        public Type fieldType(String fieldName) {
            return fieldTypes.get(fieldName);
        }

        private void discoverFields(Class<? extends AdaptiveRecord> clazz) {
            List<Field> list = $.fieldsOf(arClass, $.F.NON_STATIC_FIELD.and($.F.fieldWithAnnotation(transientAnnotationType)).negate());
            fields = new HashSet<Field>();
            fieldTypes = new HashMap<String, Type>();
            fieldGetters = new HashMap<String, Osgl.Function>();
            fieldSetters = new HashMap<String, Osgl.Func2>();
            for (Field f : list) {
                if (!f.isAnnotationPresent(transientAnnotationType)) {
                    fields.add(f);
                    fieldTypes.put(f.getName(), f.getGenericType());
                    fieldGetters.put(f.getName(), fieldGetter(f, clazz));
                    fieldSetters.put(f.getName(), fieldSetter(f, clazz));
                }
            }
        }

        private $.Func2 fieldSetter(final Field f, final Class<?> clz) {
            final String setterName = setterName(f);
            try {
                final Method m = clz.getMethod(setterName, f.getType());
                return new $.Func2() {
                    @Override
                    public Object apply(Object host, Object value) throws NotAppliedException, Osgl.Break {
                        try {
                            if (null != value && value instanceof String) {
                                Class<?> ftype = f.getType();
                                if (!ftype.isInstance(value)) {
                                    value = Act.app().resolverManager().resolve((String) value, ftype);
                                }
                            }
                            m.invoke(host, value);
                            return null;
                        } catch (IllegalAccessException e) {
                            throw E.unexpected("Class.getMethod(String) return a method[%s] that is not accessible?", m);
                        } catch (InvocationTargetException e) {
                            throw E.unexpected(e.getTargetException(), "Error invoke setter method on %s::%s", clz.getName(), setterName);
                        }
                    }
                };
            } catch (NoSuchMethodException e) {
                f.setAccessible(true);
                return new $.Func2() {
                    @Override
                    public Object apply(Object host, Object value) throws NotAppliedException, Osgl.Break {
                        try {
                            f.set(host, value);
                            return null;
                        } catch (IllegalAccessException e1) {
                            throw E.unexpected("Field[%s] is not accessible?", f);
                        }
                    }
                };
            }
        }


        private $.Function fieldGetter(final Field f, final Class<?> clz) {
            final String getterName = getterName(f);
            try {
                final Method m = clz.getMethod(getterName);
                return new $.Function() {
                    @Override
                    public Object apply(Object o) throws NotAppliedException, Osgl.Break {
                        try {
                            return m.invoke(o);
                        } catch (IllegalAccessException e) {
                            throw E.unexpected("Class.getMethod(String) return a method[%s] that is not accessible?", m);
                        } catch (InvocationTargetException e) {
                            throw E.unexpected(e.getTargetException(), "Error invoke getter method on %s::%s", clz.getName(), getterName);
                        }
                    }
                };
            } catch (NoSuchMethodException e) {
                f.setAccessible(true);
                return new $.Function() {
                    @Override
                    public Object apply(Object o) throws NotAppliedException, Osgl.Break {
                        try {
                            return f.get(o);
                        } catch (IllegalAccessException e1) {
                            throw E.unexpected("Field[%s] is not accessible?", f);
                        }
                    }
                };
            }
        }

        private String getterName(Field field) {
            boolean isBoolean = field.getType() == Boolean.class || field.getType() == boolean.class;
            return (isBoolean ? "is" : "get") + S.capFirst(field.getName());
        }

        private String setterName(Field field) {
            return "set" + S.capFirst(field.getName());
        }

        public static class Repository extends AppServicePlugin {
            @Override
            protected void applyTo(App app) {
            }

            private ConcurrentMap<Class<?>, MetaInfo> map = new ConcurrentHashMap<Class<?>, MetaInfo>();

            public MetaInfo get(Class<? extends AdaptiveRecord> clazz, $.Function<Class<? extends AdaptiveRecord>, MetaInfo> factory) {
                MetaInfo info = map.get(clazz);
                if (null == info) {
                    info = factory.apply(clazz);
                    map.putIfAbsent(clazz, info);
                }
                return info;
            }
        }
    }

}
