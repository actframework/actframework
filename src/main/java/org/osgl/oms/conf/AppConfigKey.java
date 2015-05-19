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
     * Specify {@link org.osgl.cache.CacheService Cache service} implementation
     */
    CACHE_IMPL("cache.impl"),

    /**
     * {@code oms.controller_package} specify the java
     * package where controller classes are aggregated.
     * <p>Once controller_package is specified then the application developer could
     * write short request handler in the routing table. For example, suppose an original
     * routing table is specified as</p>
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
     * <p>If {@code oms.controller_package} is specified as {@code com.mycorp.myproj.controllers}
     * then the routing table could be simplified as:</p>
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
     * {@code oms.encoding} specifies application default encoding
     * <p>Default value: utf-8</p>
     */
    ENCODING("encoding", Charsets.UTF_8.name().toLowerCase()),

    /**
     * {@code oms.host} specifies the host the application
     * reside on.
     * <p/>
     * <p>Default value: {@code localhost}</p>
     */
    HOST("host", "localhost"),

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

    /**
     * {@code ping.path} specify the ping path.
     * If this setting is specified, then when session resolving, system
     * will check if the current URL matches the setting. If matched
     * then session cookie expiration time will not be changed. Otherwise
     * the expiration time will refresh
     * <p>Default value: {@code null}</p>
     */
    PING_PATH("ping.path"),

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
     * {@code secret}
     * Specifies the secret key the application used to do general
     * encrypt/decrypt/sign etc
     * <p>Default value: {@code myawesomeapp}</p>
     */
    SECRET("secret", "myawesomeapp"),

    /**
     * {@code session.prefix} specifies the prefix to be prepended
     * to the session cookie name. Let's say the default cookie name is
     * {@code oms_session}, and user specifies the prefix {@code my_app}
     * then the session cookie name will be {@code my_app_session}.
     * <p>Note this setting also impact the {@link AppConfig#flashCookieName()}</p>
     * <p>Default value: {@code OMS}</p>
     */
    SESSION_PREFIX("session.prefix", "oms"),

    /**
     * {@code session.ttl} specifies the session duration in seconds.
     * If user failed to interact with server for amount of time that
     * exceeds the setting then the session will be destroyed
     *
     * <p>Default value: {@code 60 * 30} i.e half an hour</p>
     */
    SESSION_TTL("session.ttl", 60 * 30),

    /**
     * {@code session.persistent.enabled} specify whether the system
     * should treat session cookie as persistent cookie. If this setting
     * is enabled, then the user's session will not be destroyed after
     * browser closed.
     *
     * <p>Default value: {@code false}</p>
     *
     * See <a href="http://en.wikipedia.org/wiki/HTTP_cookie#Persistent_cookie">HTTP_cookie</a>
     */
    SESSION_PERSISTENT_ENABLED("session.persistent.enabled", false),

    /**
     * {@code session.encrypted.enabled} specify whether the system should
     * encrypt the key/value pairs in the session cookie. Enable session
     * encryption will greatly improve the security but with the cost
     * of additional CPU usage and a little bit longer time on request
     * processing.
     *
     * <p>Default value: {@code false}</p>
     */
    SESSION_ENCRYPT_ENABLED("session.encrypt.enabled", false),

    /**
     * {@code session.http_only.enabled} specifies whether the session cookie should
     * be set as http-only. Enable http only session will cause session cookie only
     * not be accessible via JavaScript.
     *
     * <p>Default value: {@code true}</p>
     */
    SESSION_HTTP_ONLY_ENABLED("session.http_only.enabled", true),

    /**
     * {@code session.secure.enabled} specifies whether the session cookie should
     * be set as secure. Enable secure session will cause session cookie only
     * effective in https connection. Literally this will enforce the web site to run
     * default by https.
     *
     * <p>Default value: {@code true}</p>
     *
     * <p><b>Note</b> when {@link OMS OMS server} is running in {@link org.osgl.oms.OMS.Mode#DEV mode}
     * session http only will be disabled without regarding to the {@code session.secure.enabled}
     * setting</p>
     */
    SESSION_SECURE("session.secure.enabled", true),

    /**
     * {@code oms.source_version} specifies the java version
     * of the srccode code. This configuration is used only
     * in dev mode.
     * <p>Default value: 1.7</p>
     */
    SOURCE_VERSION("source_version", "1." + _.JAVA_VERSION),

    /**
     * {@code template.home} specifies where the view templates resides.
     * If not specified then will use the {@link View#name() view name
     * in lower case} as the template home if that view is used.
     * <p/>
     * <p>Default value: {@code default}</p>
     */
    TEMPLATE_HOME("template.home"),

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
     * {@code view.default} specifies the default view solution. If there
     * are multiple views registered and default view are available, then
     * it will be used at priority to load the templates
     * <p/>
     * <p>Default value: {@code rythm}</p>
     */
    VIEW_DEFAULT("view.default"),

    X_FORWARD_PROTOCOL("x_forward_protocol", "http"),

    ;
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
