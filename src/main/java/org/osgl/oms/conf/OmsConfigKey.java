package org.osgl.oms.conf;

import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.oms.OMS;
import org.osgl.util.S;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link org.osgl.oms.OMS} configuration keys. General rules:
 * <p/>
 * <ul>
 * <li>When a key is ended with <code>.enabled</code>, then you should be able to set
 * the setting without <code>.enabled</code> or replace it with <code>.disabled</code>
 * but the value will be inverted. For example, <code>built_in.transformer.enabled</code>
 * is equal to <code>built_in.transformer</code> and invert to
 * <code>built_in.transformer.disabled</code></li>
 * <p/>
 * <li>When a key is ended with <code>.impl</code>, then you can either put an instance into
 * the configuration map or a string of the class className</li>
 * </ul>
 */
public enum  OmsConfigKey implements ConfigKey {

    /**
     * {@code oms.home.dir} specifies the OMS home dir
     * <p>This property must be set to start OMS. There is no default value for this configuration</p>
     */
    HOME("home.dir"),

    /**
     * {@code oms.app.base} specifies the application base in relation to the
     * {@link #HOME home dir}.
     * <p>Default value: {@code apps}</p>
     */
    APP_BASE("app.base", "apps"),

    /**
     * {@code oms.mode} specifies the OMS running mode. Options:
     * <ul>
     *     <li>{@code dev} - run OMS during development, loading and refreshing class
     *     directly from srccode code enabled in this mode</li>
     *     <li>{@code sit} - run OMS during system test</li>
     *     <li>{@code uat} - run OMS during UAT test</li>
     *     <li>{@code prod} - run OMS when system is live</li>
     * </ul>
     * <p>You pass the mode to OMS runtime during start up like:</p>
     * <pre><code>oms --mode dev</code></pre>
     * <p>Or via JVM properties like:</p>
     * <pre><code>-Dmode=uat</code></pre>
     */
    MODE("mode", OMS.Mode.PROD),
    /**
     * {@code oms.xio.impl} specifies the implementation for the network stack implementation
     */
    NETWORK_SERVER_IMPL("xio.impl")
    ;
    private static Logger logger = L.get(AppConfigKey.class);
    private static ConfigKeyHelper helper = new ConfigKeyHelper(OMS.F.MODE_ACCESSOR);

    private String key;
    private Object defVal;

    private OmsConfigKey(String key) {
        this(key, null);
    }

    private OmsConfigKey(String key, Object defVal) {
        this.key = key;
        this.defVal = defVal;
    }

    /**
     * Return the key string
     *
     * @return the key of the configuration
     */
    public String key() {
        return key;
    }

    /**
     * Return default value of this setting. The configuration data map
     * is passed in in case the default value be variable depending on
     * another setting.
     *
     * @param configuration
     * @return return the default value
     */
    protected Object getDefVal(Map<String, ?> configuration) {
        return defVal;
    }

    /**
     * Calling to this method is equals to calling {@link #key()}
     *
     * @return key of the configuration
     */
    @Override
    public String toString() {
        return key;
    }

    @Override
    public Object defVal() {
        return defVal;
    }

    public <T> List<T> implList(String key, Map<String, ?> configuration, Class<T> c) {
        return helper.getImplList(key, configuration, c);
    }

    /**
     * Return configuration value from the configuration data map using the {@link #key}
     * of this {@link AppConfigKey setting} instance
     *
     * @param configuration
     * @param <T>
     * @return return the configuration
     */
    public <T> T val(Map<String, ?> configuration) {
        return helper.getConfiguration(this, configuration);
    }

    private static Map<String, OmsConfigKey> lookup = new HashMap<String, OmsConfigKey>(50);

    static {
        for (OmsConfigKey k : values()) {
            lookup.put(k.key().toLowerCase(), k);
        }
    }

    /**
     * Return key enum instance from the string in case insensitive mode
     *
     * @param s
     * @return configuration key from the string
     */
    public static OmsConfigKey valueOfIgnoreCase(String s) {
        if (S.empty(s)) throw new IllegalArgumentException();
        return lookup.get(s.trim().toLowerCase());
    }

}
