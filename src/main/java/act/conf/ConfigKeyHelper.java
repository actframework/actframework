package act.conf;

import act.Act;
import org.osgl.$;
import org.osgl.exception.ConfigurationException;
import org.osgl.exception.NotAppliedException;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.FastStr;
import org.osgl.util.S;

import java.io.File;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * Provides configuration key enum manipulating utilities
 */
class ConfigKeyHelper {

    private static Logger logger = L.get(ConfigKeyHelper.class);
    private $.F0<Act.Mode> mode;
    private $.F0<ClassLoader> classLoaderProvider;

    public ConfigKeyHelper($.F0<Act.Mode> mode, final ClassLoader cl) {
        this.mode = mode;
        this.classLoaderProvider = new $.F0<ClassLoader>() {
            @Override
            public ClassLoader apply() throws NotAppliedException, $.Break {
                return cl;
            }
        };
    }
    public ConfigKeyHelper($.F0<Act.Mode> mode) {
        this.mode = mode;
    }
    ConfigKeyHelper classLoaderProvider($.F0<ClassLoader> provider) {
        this.classLoaderProvider = provider;
        return this;
    }
    <T> T getConfiguration(final ConfigKey confKey, Map<String, ?> configuration) {
        String key = confKey.key();
        $.F0<?> defVal = new $.F0<Object>() {
            @Override
            public Object apply() throws NotAppliedException, $.Break {
                return confKey.defVal();
            }
        };
        return getConfiguration(key, defVal, configuration);
    }
    <T> T getConfiguration(String key, $.F0<?> defVal, Map<String, ?> configuration) {
        if (key.endsWith(".enabled") || key.endsWith(".disabled")) {
            String key0 = S.beforeLast(key, ".");
            Boolean B = getEnabled(key0, configuration, defVal);
            if (null == B) {
                return null;
            }
            if (key.endsWith(".disabled")) {
                B = !B;
            }
            return (T) B;
        }
        if (key.endsWith(".impl")) {
            return getImpl(configuration, key, suffixOf(key), defVal);
        }
        if (key.endsWith(".dir") || key.endsWith(".home") || key.endsWith(".path")) {
            return (T) getUri(configuration, key, suffixOf(key), defVal);
        }
        if (key.endsWith(".bool") || key.endsWith(".boolean")) {
            return (T) getX(configuration, key, suffixOf(key), defVal, F.TO_BOOLEAN);
        }
        if (key.endsWith(".long") || key.endsWith(".ttl")) {
            return (T) getX(configuration, key, suffixOf(key), defVal, F.TO_LONG);
        }
        if (key.endsWith(".int") || key.endsWith(".len") || key.endsWith(".count") || key.endsWith(".times") || key.endsWith(".size")) {
            return (T) getX(configuration, key, suffixOf(key), defVal, F.TO_INT);
        }
        if (key.endsWith(".float")) {
            return (T) getX(configuration, key, suffixOf(key), defVal, F.TO_FLOAT);
        }
        if (key.endsWith(".double")) {
            return (T) getX(configuration, key, suffixOf(key), defVal, F.TO_DOUBLE);
        }
        return (T) getValFromAliases(configuration, key, null, defVal);
    }

    <T> List<T> getImplList(String key, Map<String, ?> configuration, Class<T> c) {
        final Object v = getValFromAliases(configuration, key, "impls", null);
        if (null == v) return Collections.EMPTY_LIST;
        final boolean needClass = (Class.class.isAssignableFrom(c));
        final List<T> l = new ArrayList<T>();
        final Class vc = v.getClass();
        if (c.isAssignableFrom(vc)) {
            l.add((T) v);
            return l;
        }
        if (v instanceof Class) {
            if (needClass) {
                l.add((T) v);
            } else {
                T inst = newInstance(key, (Class) v, c);
                if (null != inst) {
                    l.add(inst);
                }
            }
            return l;
        }
        if (vc.isArray()) {
            int len = Array.getLength(v);
            for (int i = 0; i < len; ++i) {
                Object el = Array.get(v, i);
                if (null == el) {
                    continue;
                }
                Class elc = el.getClass();
                if (c.isAssignableFrom(elc)) {
                    l.add((T) el);
                } else if (el instanceof Class) {
                    if (needClass) {
                        l.add((T) el);
                    } else {
                        T inst = newInstance(key, (Class) el, c);
                        if (null != inst) {
                            l.add(inst);
                        }
                    }
                } else {
                    try {
                        elc = Class.forName(el.toString());
                        if (needClass) {
                            l.add((T) elc);
                        } else {
                            T inst = newInstance(key, elc, c);
                            if (null != inst) {
                                l.add(inst);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn(e, "Error getting impl class out from %s for configuration %s", el, key);
                    }
                }
            }
            return l;
        } else if (Collection.class.isAssignableFrom(vc)) {
            Collection col = (Collection) v;
            for (Object el : col) {
                if (null == el) {
                    continue;
                }
                Class elc = el.getClass();
                if (c.isAssignableFrom(elc)) {
                    l.add((T) el);
                } else if (el instanceof Class) {
                    if (needClass) {
                        l.add((T) el);
                    } else {
                        T inst = newInstance(key, (Class) el, c);
                        if (null != inst) {
                            l.add(inst);
                        }
                    }
                } else {
                    try {
                        elc = Class.forName(el.toString());
                        if (needClass) {
                            l.add((T) elc);
                        } else {
                            T inst = newInstance(key, elc, c);
                            if (null != inst) {
                                l.add(inst);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn(e, "Error getting impl class out from %s for configuration %s", el, key);
                    }
                }
            }
            return l;
        }

        for (String s : v.toString().split("[ \t,;]+")) {
            try {
                Class ec = Class.forName(s);
                if (needClass) {
                    l.add((T) ec);
                } else {
                    T inst = newInstance(key, ec, c);
                    if (null != inst) {
                        l.add(inst);
                    }
                }
            } catch (Exception e) {
                logger.warn(e, "Error getting impl class out from %s for configuration %s", s, key);
            }
        }

        return l;
    }

    <T> T getX(Map<String, ?> configuration, String key, String suffix, $.F0<?> defVal, $.Func1<Object, T> converter) {
        Object v = getValFromAliases(configuration, key, suffix, defVal);
        return converter.apply(v);
    }

    private Boolean getEnabled(String key, Map<String, ?> configuration, $.F0<?> defVal) {
        Object v = getValFromAliases(configuration, key, "enabled", defVal);
        if (null == v) {
            v = getValFromAliases(configuration, key, "disabled", defVal);
            return null == v ? null : !toBoolean(v);
        }
        return toBoolean(v);
    }

    private ClassLoader myClassLoader() {
        return classLoaderProvider.apply();
    }

    private <T> T getImpl(Map<String, ?> configuration, String key, String suffix, $.F0<?> defVal) {
        Object v = getValFromAliases(configuration, key, "impl", defVal);
        if (null == v) return null;
        if (v instanceof Class) {
            try {
                return $.newInstance((Class<T>) v, myClassLoader());
            } catch (Exception e) {
                throw new ConfigurationException(e, "Error getting implementation configuration: %s", key);
            }
        }
        if (!(v instanceof String)) return (T) v;
        String clsName = (String) v;
        try {
            return $.newInstance(clsName, myClassLoader());
        } catch (Exception e) {
            throw new ConfigurationException(e, "Error getting implementation configuration: %s", key);
        }
    }

    private URI getUri(Map<String, ?> configuration, String key, String suffix, $.F0<?> defVal) {
        Object v = getValFromAliases(configuration, key, suffix, defVal);
        if (null == v) return null;
        if (v instanceof File) {
            return ((File) v).toURI();
        }
        String s = v.toString();
        return asUri(s, key);
    }

    private static URI asUri(String s, String key) {
        boolean isAbsolute = false;
        if (s.startsWith("/") || s.startsWith(File.separator)) {
            isAbsolute = true;
        } else if (s.matches("^[a-zA-Z]:.*")) {
            isAbsolute = true;
        }
        if (isAbsolute) {
            File f = new File(s);
            if (f.exists() && f.isDirectory() && f.canRead()) {
                return f.toURI();
            }
            return null;
        }
        try {
            if (s.startsWith("..")) {
                URL url = Thread.currentThread().getContextClassLoader().getResource(".");
                if (null == url) {
                    // must running from inside a jar file, and it doesn't support
                    // template root starts with ".."
                    return null;
                }
                String path = url.getPath();
                if (path.endsWith("/")) path = path + s;
                else path = path + "/" + s;
                return new URI(path);
            } else {
                URL url = Thread.currentThread().getContextClassLoader().getResource(s);
                return null == url ? null : url.toURI();
            }
        } catch (Exception e) {
            throw new ConfigurationException(e, "Error reading file configuration %s", key);
        }
    }

    private static Boolean toBoolean(Object v) {
        if (null == v) return null;
        if (v instanceof Boolean) return (Boolean) v;
        return Boolean.parseBoolean(v.toString());
    }

    private static Long toLong(Object v) {
        if (null == v) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.parseLong(v.toString());
    }

    private static Integer toInt(Object v) {
        if (null == v) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        return Integer.parseInt(v.toString());
    }

    private static Float toFloat(Object v) {
        if (null == v) return null;
        if (v instanceof Number) return ((Number) v).floatValue();
        return Float.parseFloat(v.toString());
    }

    private static Double toDouble(Object v) {
        if (null == v) return null;
        if (v instanceof Number) return ((Number) v).doubleValue();
        return Double.parseDouble(v.toString());
    }

    private Object getValFromAliases(Map<String, ?> configuration, String key, String suffix) {
        Object v = configuration.get(key);
        if (null == v) {
            for (String k0 : aliases(key, suffix)) {
                v = configuration.get(k0);
                if (null != v) break;
            }
        } else if (v instanceof String) {
            String vs = v.toString();
            if (vs.startsWith("$")) {
                FastStr s = FastStr.of(v.toString());
                String k = s.afterFirst("{").beforeFirst("}").toString();
                String nv = System.getProperty(k);
                if (null == nv) {
                    nv = System.getenv(k);
                }
                if (null != nv) {
                    v = nv;
                }
            }
        }
        return v;
    }

    private Object getValFromAliasesWithModelPrefix(Map<String, ?> configuration, String key, String suffix) {
        return getValFromAliases(configuration, mode().configKey(key), suffix);
    }

    Object getValFromAliases(Map<String, ?> configuration, String key, String suffix, $.F0<?> defVal) {
        Object v = getValFromAliasesWithModelPrefix(configuration, key, suffix);
        if (null != v) {
            return v;
        }
        v = getValFromAliases(configuration, key, suffix);
        if (null != v) {
            return v;
        }
        // still not found, load default value
        v = defVal.apply();
        return v;
    }

    private Set<String> aliases(String key, String suffix) {
        Set<String> set = C.newSet();
        set.add(Config.PREFIX + key);
        set.add(key);
        if (S.notBlank(suffix)) {
            if (key.contains(suffix)) {
                String k0 = key.replace("." + suffix, "");
                set.add(Config.PREFIX + k0);
                set.add(k0);
            } else {
                if (!suffix.startsWith(".")) {
                    suffix = "." + suffix;
                }
                String k0 = S.builder(key).append(suffix).toString();
                set.add(Config.PREFIX + k0);
                set.add(k0);
            }
        }
        return set;
    }

    private Act.Mode mode() {
        return mode.apply();
    }

    private String suffixOf(String key) {
        return S.afterLast(key, ".");
    }

    private static <T> T newInstance(String key, Class c, Class<T> expectedClass) {
        if (!expectedClass.isAssignableFrom(c)) {
            logger.warn("Mismatched type found for configuration %s", key);
            return null;
        }
        try {
            return (T) c.newInstance();
        } catch (Exception e) {
            logger.warn(e, "Cannot create new instance for configuration %s", key);
            return null;
        }
    }

    public enum F {
        ;
        public static $.Func1<Object, Integer> TO_INT = new $.Transformer<Object, Integer>() {
            @Override
            public Integer transform(Object object) {
                return toInt(object);
            }
        };
        public static $.Func1<Object, Long> TO_LONG = new $.Transformer<Object, Long>() {
            @Override
            public Long transform(Object object) {
                return toLong(object);
            }
        };
        public static $.Func1<Object, Float> TO_FLOAT = new $.Transformer<Object, Float>() {
            @Override
            public Float transform(Object object) {
                return toFloat(object);
            }
        };
        public static $.Func1<Object, Double> TO_DOUBLE = new $.Transformer<Object, Double>() {
            @Override
            public Double transform(Object object) {
                return toDouble(object);
            }
        };
        public static $.Func1<Object, Boolean> TO_BOOLEAN = new $.Transformer<Object, Boolean>() {
            @Override
            public Boolean transform(Object object) {
                return toBoolean(object);
            }
        };
    }

}
