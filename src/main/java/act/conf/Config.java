package act.conf;

import act.util.DestroyableBase;
import org.osgl.util.C;

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
    public <T> T get(ConfigKey key) {
        Object o = data.get(key);
        if (null == o) {
            o = key.val(raw);
            if (null != o) {
                data.put(key, o);
            } else {
                data.put(key, NULL);
            }
        }
        if (o == NULL) {
            return null;
        } else {
            return (T) o;
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
            return get(rk);
        } else {
            return (T) raw.get(key);
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
        for (Map.Entry<String, Object> entries: raw.entrySet()) {
            if (entries.getKey().toLowerCase().endsWith(key)) {
                return (T) entries.getValue();
            }
        }
        return null;
    }

    public Map rawConfiguration() {
        return raw;
    }

    public Map subSet(String prefix) {
        String prefix2 = "act." + prefix;
        Map<String, Object> subset = C.newMap();
        for (String key : raw.keySet()) {
            if (key.startsWith(prefix) || key.startsWith(prefix2)) {
                Object o = data.get(key);
                if (null == o) o = raw.get(key);
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
