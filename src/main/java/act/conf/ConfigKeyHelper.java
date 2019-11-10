package act.conf;

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
import org.osgl.$;
import org.osgl.exception.ConfigurationException;
import org.osgl.exception.NotAppliedException;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;
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
    private ClassLoader cl;
    private boolean useAppClassLoader;

    public ConfigKeyHelper($.F0<Act.Mode> mode, final ClassLoader cl) {
        this.mode = mode;
        this.cl = $.requireNotNull(cl);
    }
    public ConfigKeyHelper($.F0<Act.Mode> mode) {
        this.mode = mode;
    }

    ConfigKeyHelper onApp() {
        this.useAppClassLoader = true;
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
        key = Config.canonical(key);
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
        if (key.endsWith(".long")) {
            return (T) getX(configuration, key, suffixOf(key), defVal, F.TO_LONG);
        }
        if (key.endsWith(".int") || key.endsWith(".ttl") || key.endsWith(".len") || key.endsWith(".count") || key.endsWith(".times") || key.endsWith(".size") || key.endsWith(".port") || key.endsWith(".timeout")) {
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

    private static final Set<String> SUFFIXES = C.set(S.fastSplit("enabled,disabled,impl,dir,home,path,bool,boolean,long,ttl,port,int,len,count,times,size,float,double", ","));
    static Set<String> suffixes() {
        return SUFFIXES;
    }

    private static final Set<String> NON_ALIAS_SUFFIXES = C.set(S.fastSplit("dir,home,path,ttl,port,len,count,times,size,timeout", ","));
    static Set<String> nonAliasSuffixes() {
        return NON_ALIAS_SUFFIXES;
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
        return cl;
    }

    <T> T getImpl(Map<String, ?> configuration, String key, String suffix, $.F0<?> defVal) {
        Object v = getValFromAliases(configuration, key, "impl", defVal);
        if (null == v) return null;
        if (v instanceof Class) {
            try {
                return instanceOf((Class<T>) v);
            } catch (Exception e) {
                throw new ConfigurationException(e, "Error getting implementation configuration: %s", key);
            }
        }
        if (!(v instanceof String)) return (T) v;
        String clsName = (String) v;
        try {
            return instanceOf(clsName);
        } catch (Exception e) {
            throw new ConfigurationException(e, "Error getting implementation configuration: %s", key);
        }
    }

    private <T> T instanceOf(Class<T> type) {
        return useAppClassLoader ? Act.getInstance(type) : $.newInstance(type, myClassLoader());
    }

    private <T> T instanceOf(String typeName) {
        return (T) (useAppClassLoader ? Act.getInstance(typeName) : $.newInstance(typeName, myClassLoader()));
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
        String s = v.toString();
        if (s.contains("*")) {
            List<String> sl = S.fastSplit(s, "*");
            int n = 1;
            for (String sn : sl) {
                n *= Integer.parseInt(sn.trim());
            }
            return n;
        }
        return Integer.parseInt(s);
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
        }
        if (null != v && v instanceof String) {
            v = evaluate((String) v, configuration);
        }
        return v;
    }

    /*
     * Check if v has variable e.g. `${foo.bar}` inside and expand it recursively
     */
    public String evaluate(String s, Map<String, ?> map) {
        int n = 0, n0 = 0, len = s.length();
        S.Buffer sb = S.newBuffer();
        while (n > -1 && n < len) {
            n = s.indexOf("${", n);
            if (n < 0) {
                if (n0 == 0) {
                    return s;
                }
                sb.append(s.substring(n0, len));
                break;
            }
            sb.append(s.substring(n0, n));
            // now search for "}"
            n += 2;
            n0 = n;
            n = s.indexOf("}", n0 + 1);
            if (n < 0) {
                logger.warn("Invalid expression found in the configuration value: %s", s);
                return s;
            }
            String expression = s.substring(n0, n);
            if (S.notBlank(expression)) {
                // in case getting from Env variable
                Object o = map.get(expression);
                if (null == o) {
                    o = getConfiguration(expression, null, map);
                }
                if (null != o) {
                    sb.append(o);
                } else {
                    logger.warn("Cannot find expression value for: %s", expression);
                }
            }
            n += 1;
            n0 = n;
        }
        return sb.toString();
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
        if (null != defVal) {
            v = defVal.apply();
        }
        return v;
    }

    static Set<String> aliases(String key, String suffix) {
        Set<String> set = new HashSet<>();
        set.add(Config.PREFIX + key);
        set.add(key);
        if (S.notBlank(suffix) && !nonAliasSuffixes().contains(suffix)) {
            if (key.contains(suffix)) {
                String k0 = key.replace("." + suffix, "");
                set.add(Config.PREFIX + k0);
                set.add(k0);
            } else {
                if (!suffix.startsWith(".")) {
                    suffix = "." + suffix;
                }
                String k0 = key + suffix;
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
