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

import act.util.DestroyableBase;
import org.osgl.exception.ConfigurationException;
import org.osgl.exception.UnexpectedNewInstanceException;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for XxConfig
 */
public abstract class Config<E extends ConfigKey> extends DestroyableBase {

    static final String PREFIX = "act.";
    static final int PREFIX_LEN = PREFIX.length();

    protected Map<String, Object> raw;
    protected Map<ConfigKey, Object> data;

    /**
     * Construct a <code>AppConfig</code> with a map. The map is copied to
     * the original map of the configuration instance
     *
     * @param configuration
     */
    public Config(Map<String, ?> configuration) {
        raw = new HashMap<String, Object>(configuration);
        data = new HashMap<ConfigKey, Object>(configuration.size());
    }

    public Config() {
        this((Map) System.getProperties());
    }

    @Override
    protected void releaseResources() {
        raw.clear();
        data.clear();
        super.releaseResources();
    }

    /**
     * Return configuration by {@link AppConfigKey configuration key}
     *
     * @param key
     * @param <T>
     * @return the configured item
     */
    public <T> T get(ConfigKey key, T def) {
        Object o = data.get(key);
        if (null == o) {
            o = key.val(raw);
            if (null == o) {
                o = null == def ? NULL : def;
            }
            data.put(key, o);
        }
        if (o == NULL) {
            return null;
        } else {
            return (T) o;
        }
    }

    public void set(ConfigKey key, Object val) {
        data.put(key, val);
    }

    public Integer getInteger(ConfigKey key, Integer def) {
        Object retVal = get(key, def);
        if (null == retVal) {
            return null;
        }
        if (retVal instanceof Number) {
            return ((Number) retVal).intValue();
        }
        String s = S.string(retVal);
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

    boolean hasConfiguration(ConfigKey key) {
        Object o = data.get(key);
        if (null != o && NULL != o) {
            return true;
        }
        try {
            o = key.val(raw);
            if (null == o) {
                return false;
            }
            return true;
        } catch (ConfigurationException e) {
            Throwable t = e.getCause();
            if (t instanceof UnexpectedNewInstanceException) {
                // assume this is caused by certain `.impl` setting which are not a class
                return true;
            }
            // we don't know what it is so just rethrow it
            throw e;
        }
    }

    /**
     * Return a configuration value as list
     *
     * @param key
     * @param c
     * @param <T>
     * @return the list
     */
    public <T> List<T> getList(AppConfigKey key, Class<T> c) {
        Object o = data.get(key);
        if (null == o) {
            List<T> l = key.implList(key.key(), raw, c);
            data.put(key, l);
            return l;
        } else {
            return (List) o;
        }
    }

    /**
     * Look up configuration by a <code>String<code/> key. If the String key
     * can be converted into {@link AppConfigKey rythm configuration key}, then
     * it is converted and call to {@link #get(ConfigKey)} method. Otherwise
     * the original configuration map is used to fetch the value from the string key
     *
     * @param key
     * @param <T>
     * @return the configured item
     */
    public <T> T get(String key) {
        if (key.startsWith(PREFIX)) {
            key = key.substring(PREFIX_LEN);
        }
        ConfigKey rk = keyOf(key);
        if (null != rk) {
            return get(rk, null);
        } else {
            return AppConfigKey.helper.getConfiguration(key, null, raw);
        }
    }

    public <T> T getIgnoreCase(String key) {
        if (key.startsWith(PREFIX)) {
            key = key.substring(PREFIX_LEN);
        }
        T t = get(key);
        if (null != t) {
            return t;
        }
        key = key.toLowerCase();
        for (Map.Entry<String, Object> entries : raw.entrySet()) {
            if (entries.getKey().toLowerCase().equals(key)) {
                Object o = entries.getValue();
                if (o instanceof String) {
                    return (T) AppConfigKey.helper.evaluate(o.toString(), raw);
                }
                return (T) o;
            } else if (entries.getKey().replace('_', '.').equals(key.replace('_', '.'))) {
                Object o = entries.getValue();
                if (o instanceof String) {
                    return (T) AppConfigKey.helper.evaluate(o.toString(), raw);
                }
                return (T) o;
            }
        }
        return null;
    }

    public Map<String, Object> rawConfiguration() {
        return raw;
    }

    public Map<String, Object> subSet(String namespace) {
        if (!namespace.endsWith(".")) {
            namespace = namespace + ".";
        }
        String prefix2 = "act." + namespace;
        Map<String, Object> subset = C.newMap();
        for (String key : raw.keySet()) {
            if (key.startsWith(namespace) || key.startsWith(prefix2)) {
                Object o = raw.get(key);
                if (null == o) {
                    continue;
                }
                if (o instanceof String) {
                    o = AppConfigKey.helper.evaluate(o.toString(), raw);
                }
                if (key.startsWith("act.")) {
                    key = key.substring(4);
                }
                if (subset.containsKey(key)) continue;
                subset.put(key, o);
            }
        }
        return subset;
    }

    protected abstract ConfigKey keyOf(String s);

    private static final Object NULL = new Object();
}
