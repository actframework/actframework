package act.conf;

import act.Act;
import org.osgl._;
import org.osgl.exception.ConfigurationException;
import org.osgl.exception.NotAppliedException;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
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
    private _.F0<Act.Mode> mode;

    public ConfigKeyHelper(_.F0<Act.Mode> mode) {
        this.mode = mode;
    }

    <T> T getConfiguration(final ConfigKey confKey, Map<String, ?> configuration) {
        String key = confKey.key();
        _.F0<?> defVal = new _.F0<Object>() {
            @Override
            public Object apply() throws NotAppliedException, _.Break {
                return confKey.defVal();
            }
        };
        if (key.endsWith(".enabled") || key.endsWith(".disabled")) {
            return (T) getEnabled(key, configuration, defVal);
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
        if (key.endsWith(".int") || key.endsWith(".count") || key.endsWith(".times") || key.endsWith(".size")) {
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

    private <T> T getX(Map<String, ?> configuration, String key, String suffix, _.F0<?> defVal, _.Func1<Object, T> converter) {
        Object v = getValFromAliases(configuration, key, suffix, defVal);
        return converter.apply(v);
    }

    private Boolean getEnabled(String key, Map<String, ?> configuration, _.F0<?> defVal) {
        Object v = getValFromAliases(configuration, key, "enabled", defVal);
        if (null == v) {
            v = getValFromAliases(configuration, key, "disabled", defVal);
            return !toBoolean(v);
        }
        return toBoolean(v);
    }

    private <T> T getImpl(Map<String, ?> configuration, String key, String suffix, _.F0<?> defVal) {
        Object v = getValFromAliases(configuration, key, "impl", defVal);
        if (null == v) return null;
        if (v instanceof Class) {
            try {
                return _.newInstance((Class<T>) v);
            } catch (Exception e) {
                throw new ConfigurationException(e, "Error getting implementation configuration: %s", key);
            }
        }
        if (!(v instanceof String)) return (T) v;
        String clsName = (String) v;
        try {
            return _.newInstance(clsName);
        } catch (Exception e) {
            throw new ConfigurationException(e, "Error getting implementation configuration: %s", key);
        }
    }

    private URI getUri(Map<String, ?> configuration, String key, String suffix, _.F0<?> defVal) {
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

    private static boolean toBoolean(Object v) {
        if (null == v) return false;
        if (v instanceof Boolean) return (Boolean) v;
        return Boolean.parseBoolean(v.toString());
    }

    private static long toLong(Object v) {
        if (null == v) return 0l;
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.parseLong(v.toString());
    }

    private static int toInt(Object v) {
        if (null == v) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        return Integer.parseInt(v.toString());
    }

    private static float toFloat(Object v) {
        if (null == v) return 0f;
        if (v instanceof Number) return ((Number) v).floatValue();
        return Float.parseFloat(v.toString());
    }

    private static double toDouble(Object v) {
        if (null == v) return 0d;
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
        return v;
    }

    private Object getValFromAliasesWithModelPrefix(Map<String, ?> configuration, String key, String suffix) {
        return getValFromAliases(configuration, mode().configKey(key), suffix);
    }

    private Object getValFromAliases(Map<String, ?> configuration, String key, String suffix, _.F0<?> defVal) {
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

    private List<String> aliases(String key, String suffix) {
        List<String> l = new ArrayList<String>();
        l.add(Config.PREFIX + key);
        l.add(key);
        if (S.notBlank(suffix)) {
            String k0 = key.replace("." + suffix, "");
            l.add(Config.PREFIX + k0);
            l.add(k0);
        }
        return l;
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

    private static enum F {
        ;
        static _.Func1<Object, Integer> TO_INT = new _.Transformer<Object, Integer>() {
            @Override
            public Integer transform(Object object) {
                return toInt(object);
            }
        };
        static _.Func1<Object, Long> TO_LONG = new _.Transformer<Object, Long>() {
            @Override
            public Long transform(Object object) {
                return toLong(object);
            }
        };
        static _.Func1<Object, Float> TO_FLOAT = new _.Transformer<Object, Float>() {
            @Override
            public Float transform(Object object) {
                return toFloat(object);
            }
        };
        static _.Func1<Object, Double> TO_DOUBLE = new _.Transformer<Object, Double>() {
            @Override
            public Double transform(Object object) {
                return toDouble(object);
            }
        };
        static _.Func1<Object, Boolean> TO_BOOLEAN = new _.Transformer<Object, Boolean>() {
            @Override
            public Boolean transform(Object object) {
                return toBoolean(object);
            }
        };
    }

}
