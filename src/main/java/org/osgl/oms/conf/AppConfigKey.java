package org.osgl.oms.conf;

import org.apache.commons.codec.Charsets;
import org.osgl._;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.oms.OMS;
import org.osgl.oms.view.View;
import org.osgl.util.S;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@link org.osgl.oms.app.App} configuration keys. General rules:
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
public enum AppConfigKey implements ConfigKey {

    /**
     * {@code oms.encoding} specifies application default encoding
     * <p>Default value: utf-8</p>
     */
    ENCODING("encoding", Charsets.UTF_8.name().toLowerCase()),

    /**
     * {@code oms.locale} specifies the application default locale
     * <p>Default value: {@link java.util.Locale#getDefault}</p>
     */
    LOCALE("locale", Locale.getDefault()) {
        @Override
        public <T> T val(Map<String, ?> configuration) {
            Object o = super.val(configuration);
            if (null == o) {
                return null;
            }
            if (o instanceof String) {
                return (T) Locale.forLanguageTag((String) o);
            } else if (o instanceof Locale) {
                return (T) o;
            } else {
                String s = o.toString();
                return (T) Locale.forLanguageTag(s);
            }
        }
    },

    /**
     * {@code oms.source_version} specifies the java version
     * of the srccode code. This configuration is used only
     * in dev mode.
     * <p>Default value: 1.7</p>
     */
    SOURCE_VERSION("source_version", "1." + _.JAVA_VERSION),

    /**
     * {@code oms.url_context} specifies the context part
     * of the URL. This is used for OMS to dispatch the
     * incoming request to the application. Usually
     * the {@link #PORT port} configuration is preferred
     * than this configuration
     * <p/>
     * <p>Default value is empty string</p>
     */
    URL_CONTEXT("url_context"),

    /**
     * {@code oms.host} specifies the host the application
     * reside on.
     * <p/>
     * <p>Default value: {@code localhost}</p>
     */
    HOST("host", "localhost"),

    /**
     * {@code oms.port} specifies the port the application
     * listen to. This is preferred way to dispatch the
     * request to the application.
     * <p/>
     * <p>Default value: {@code 5460}</p>
     */
    PORT("port", 5460) {
        @Override
        public <T> T val(Map<String, ?> configuration) {
            Object v = configuration.get(key());
            if (null == v) return (T) (Number) 5460;
            if (v instanceof Number) {
                return (T) v;
            }
            return (T) (Integer.valueOf(v.toString()));
        }
    },

    X_FORWARD_PROTOCOL("x_forward_protocol", "http"),

    /**
     * {@code oms.controller_package} specify the java
     * package where controller classes are aggregated.
     * <p/>
     * <p>Once controller_package is specified then the application developer could
     * write short request handler in the routing table. For example, if an original
     * routing table is specified as</p>
     * <p/>
     * <table>
     * <tr>
     * <th>HTTP Method</th>
     * <th>URL Path</th>
     * <th>Action handler</th>
     * </tr>
     * <tr>
     * <td>GET</td>
     * <td>/users</td>
     * <td>com.mycorp.myproj.controllers.UserController.list</td>
     * </tr>
     * </table>
     * <p/>
     * <p>If {@code oms.controller_package} is specified as {@code com.mycorp.myproj.controllers}
     * then the routing table could be simplified as:</p>
     * <p/>
     * <p/>
     * <table>
     * <tr>
     * <th>HTTP Method</th>
     * <th>URL Path</th>
     * <th>Action handler</th>
     * </tr>
     * <tr>
     * <td>GET</td>
     * <td>/users</td>
     * <td>UserController.list</td>
     * </tr>
     * </table>
     */
    CONTROLLER_PACKAGE("controller_package"),

    /**
     * {@code scan_package}
     * Specify the app package in which all classes is subject
     * to bytecode processing, e.g enhancement and injection.
     * This setting should be specified when application loaded.
     * Otherwise OMS will try to process all classes found in
     * application's lib and classes folder, which might cause
     * performance issue on loading
     */
    SCAN_PACKAGE("scan_package"),

    /**
     * {@code template.home} specifies where the view templates resides.
     * If not specified then will use the {@link View#name() view name
     * in lower case} as the template home if that view is used.
     * <p/>
     * <p>Default value: {@code default}</p>
     */
    TEMPLATE_HOME("template.home"),

    /**
     * {@code view.default} specifies the default view solution. If there
     * are multiple views registered and default view are available, then
     * it will be used at priority to load the templates
     * <p/>
     * <p>Default value: {@code rythm}</p>
     */
    VIEW_DEFAULT("view.default"),

    /**
     * {@code template_path_resolver.impl} specifies the class that
     * implements {@link org.osgl.oms.view.TemplatePathResolver}. Application
     * developer could use this configuration to add some flexibility to
     * template path resolving logic, e.g. different home for different locale
     * or different home for different device type etc.
     * <p/>
     * <p>Default value: {@link org.osgl.oms.view.TemplatePathResolver}</p>
     */
    TEMPLATE_PATH_RESOLVER("template_path_resolver.impl"),

    /**
     * Specify {@link org.osgl.cache.CacheService Cache service} implementation
     */
    CACHE_IMPL("cache.impl"),;

    private String key;
    private Object defVal;
    private static Logger logger = L.get(AppConfigKey.class);
    private static ConfigKeyHelper helper = new ConfigKeyHelper(OMS.F.MODE_ACCESSOR);

    private AppConfigKey(String key) {
        this(key, null);
    }

    private AppConfigKey(String key, Object defVal) {
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

    private static Map<String, AppConfigKey> lookup = new HashMap<String, AppConfigKey>(50);

    static {
        for (AppConfigKey k : values()) {
            lookup.put(k.key().toLowerCase(), k);
        }
    }

    /**
     * Return key enum instance from the string in case insensitive mode
     *
     * @param s
     * @return configuration key from the string
     */
    public static AppConfigKey valueOfIgnoreCase(String s) {
        if (S.empty(s)) throw new IllegalArgumentException();
        return lookup.get(s.trim().toLowerCase());
    }

}
