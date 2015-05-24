package act.conf;

import act.Constants;
import act.Act;
import act.view.TemplatePathResolver;
import act.view.View;
import org.osgl._;
import org.osgl.cache.CacheService;
import org.osgl.cache.CacheServiceProvider;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.S;

import java.util.Locale;
import java.util.Map;

public class AppConfig extends Config<AppConfigKey> {

    private static Logger logger = L.get(AppConfig.class);

    public static final String CONF_FILE_NAME = "app.conf";

    /**
     * Construct a <code>AppConfig</code> with a map. The map is copied to
     * the original map of the configuration instance
     *
     * @param configuration
     */
    public AppConfig(Map<String, ?> configuration) {
        super(configuration);
    }

    public AppConfig() {
        this((Map) System.getProperties());
    }

    @Override
    protected ConfigKey keyOf(String s) {
        return AppConfigKey.valueOfIgnoreCase(s);
    }

    private String urlContext = null;

    public String urlContext() {
        if (urlContext == null) {
            urlContext = get(AppConfigKey.URL_CONTEXT);
            if (null == urlContext) {
                urlContext = "/";
            }
        }
        return urlContext;
    }

    private String xForwardedProtocol = null;

    public String xForwardedProtocol() {
        if (null == xForwardedProtocol) {
            xForwardedProtocol = get(AppConfigKey.X_FORWARD_PROTOCOL);
        }
        return xForwardedProtocol;
    }

    private String controllerPackage = null;

    public String controllerPackage() {
        if (null == controllerPackage) {
            controllerPackage = get(AppConfigKey.CONTROLLER_PACKAGE);
        }
        return controllerPackage;
    }

    private String host = null;

    public String host() {
        if (null == host) {
            host = get(AppConfigKey.HOST);
        }
        return host;
    }

    private int port = -1;

    public int port() {
        if (-1 == port) {
            port = get(AppConfigKey.PORT);
        }
        return port;
    }

    private String encoding = null;

    public String encoding() {
        if (null == encoding) {
            encoding = get(AppConfigKey.ENCODING);
        }
        return encoding;
    }

    private Locale locale = null;

    public Locale locale() {
        if (null == locale) {
            locale = get(AppConfigKey.LOCALE);
        }
        return locale;
    }

    private String sourceVersion = null;

    public String sourceVersion() {
        if (null == sourceVersion) {
            sourceVersion = get(AppConfigKey.SOURCE_VERSION);
        }
        return sourceVersion;
    }

    private _.Predicate<String> APP_CLASS_TESTER = null;

    private _.Predicate<String> appClassTester() {
        if (null == APP_CLASS_TESTER) {
            String scanPackage = get(AppConfigKey.SCAN_PACKAGE);
            if (S.isBlank(scanPackage)) {
                APP_CLASS_TESTER = _.F.yes();
            } else {
                final String[] sp = scanPackage.trim().split(Constants.LIST_SEPARATOR);
                final int len = sp.length;
                if (1 == len) {
                    APP_CLASS_TESTER = S.F.startsWith(sp[0]);
                } else {
                    APP_CLASS_TESTER = new _.Predicate<String>() {
                        @Override
                        public boolean test(String className) {
                            for (int i = 0; i < len; ++i) {
                                if (className.startsWith(sp[i])) {
                                    return true;
                                }
                            }
                            return false;
                        }
                    };
                }
            }
        }
        return APP_CLASS_TESTER;
    }

    public boolean needEnhancement(String className) {
        return appClassTester().test(className) || controllerNameTester().test(className);
    }

    private _.Predicate<String> CONTROLLER_CLASS_TESTER = null;

    private _.Predicate<String> controllerNameTester() {
        if (null == CONTROLLER_CLASS_TESTER) {
            String controllerPackage = get(AppConfigKey.CONTROLLER_PACKAGE);
            if (S.isBlank(controllerPackage)) {
                CONTROLLER_CLASS_TESTER = _.F.no();
            } else {
                final String cp = controllerPackage.trim();
                return S.F.startsWith(cp);
            }
        }
        return CONTROLLER_CLASS_TESTER;
    }

    private TemplatePathResolver templatePathResolver = null;

    public TemplatePathResolver templatePathResolver() {
        if (null == templatePathResolver) {
            templatePathResolver = get(AppConfigKey.TEMPLATE_PATH_RESOLVER);
            if (null == templatePathResolver) {
                templatePathResolver = new TemplatePathResolver();
            }
        }
        return templatePathResolver;
    }

    private String templateHome = null;

    public String templateHome() {
        if (null == templateHome) {
            templateHome = get(AppConfigKey.TEMPLATE_HOME);
            if (S.blank(templateHome)) {
                templateHome = "default";
            }
        }
        return templateHome;
    }

    private String defViewName = null;
    private View defView = null;
    public View defaultView() {
        if (null == defViewName) {
            defViewName = get(AppConfigKey.VIEW_DEFAULT);
            defView = Act.viewManager().view(defViewName);
        }
        return defView;
    }

    private boolean pingPathResolved = false;
    private String pingPath = null;
    public String pingPath() {
        if (!pingPathResolved) {
            pingPath = get(AppConfigKey.PING_PATH);
            pingPathResolved = true;
        }
        return pingPath;
    }

    private String sessionCookieName = null;
    public String sessionCookieName() {
        if (null == sessionCookieName) {
            String sessionCookiePrefix = get(AppConfigKey.SESSION_PREFIX);
            sessionCookieName = S.builder(sessionCookiePrefix).append("_").append("session").toString();
        }
        return sessionCookieName;
    }

    private String flashCookieName = null;
    public String flashCookieName() {
        if (null == flashCookieName) {
            String sessionCookiePrefix = get(AppConfigKey.SESSION_PREFIX);
            flashCookieName = S.builder(sessionCookiePrefix).append("_").append("flash").toString();
        }
        return flashCookieName;
    }

    private Long sessionTtl = null;
    public long sessionTtl() {
        if (null == sessionTtl) {
            sessionTtl = get(AppConfigKey.SESSION_TTL);
        }
        return sessionTtl;
    }

    private Boolean sessionPersistent = null;
    public boolean persistSession() {
        if (null == sessionPersistent) {
            sessionPersistent = get(AppConfigKey.SESSION_PERSISTENT_ENABLED);
        }
        return sessionPersistent;
    }

    private Boolean sessionEncrypt = null;
    public boolean encryptSession() {
        if (null == sessionEncrypt) {
            sessionEncrypt = get(AppConfigKey.SESSION_ENCRYPT_ENABLED);
        }
        return sessionEncrypt;
    }

    private Boolean sessionHttpOnly = null;
    public boolean sessionHttpOnly() {
        if (null == sessionHttpOnly) {
            sessionHttpOnly = get(AppConfigKey.SESSION_HTTP_ONLY_ENABLED);
        }
        return sessionHttpOnly;
    }

    private Boolean sessionSecure = null;
    public boolean sessionSecure() {
        if (Act.isDev()) {
            return false;
        }
        if (null == sessionSecure) {
            sessionSecure = get(AppConfigKey.SESSION_HTTP_ONLY_ENABLED);
        }
        return sessionSecure;
    }

    private String secret = null;
    public String secret() {
        if (null == secret) {
            secret = get(AppConfigKey.SECRET);
            if ("myawesomeapp".equals(secret)) {
                logger.warn("Application secret key not set! You are in the dangerous zone!!!");
            }
        }
        return secret;
    }

    public boolean possibleControllerClass(String className) {
        return controllerNameTester().test(className);
    }

    private CacheServiceProvider csp = null;

    public CacheService cacheService(String name) {
        if (null == csp) {
            csp = CacheServiceProvider.Impl.Simple;
        }
        return csp.get(name);
    }
}
