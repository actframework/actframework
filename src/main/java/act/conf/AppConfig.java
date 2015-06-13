package act.conf;

import act.Act;
import act.Constants;
import act.app.App;
import act.app.AppHolder;
import act.app.conf.AppConfigurator;
import act.util.JavaVersion;
import act.view.TemplatePathResolver;
import act.view.View;
import org.apache.commons.codec.Charsets;
import org.osgl._;
import org.osgl.cache.CacheService;
import org.osgl.cache.CacheServiceProvider;
import org.osgl.exception.ConfigurationException;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.E;
import org.osgl.util.FastStr;
import org.osgl.util.S;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static act.conf.AppConfigKey.*;

public class AppConfig<T extends AppConfigurator> extends Config<AppConfigKey> implements AppHolder<AppConfig<T>> {

    protected static Logger logger = L.get(AppConfig.class);

    public static final String CONF_FILE_NAME = "app.conf";

    private App app;
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

    public AppConfig<T> app(App app) {
        E.NPE(app);
        this.app = app;
        return this;
    }

    public App app() {
        return app;
    }

    @Override
    protected ConfigKey keyOf(String s) {
        return AppConfigKey.valueOfIgnoreCase(s);
    }

    private AppConfigurator configurator;
    private boolean configuratorLoaded = false;
    public AppConfigurator appConfigurator() {
        if (!configuratorLoaded) {
            configurator = get(CONFIG_IMPL);
            configuratorLoaded = true;
        }
        return configurator;
    }

    private String urlContext = null;
    protected T urlContext(String context) {
        if (S.blank(context)) {
            urlContext = "/";
        } else {
            urlContext = context.trim();
        }
        return me();
    }
    public String urlContext() {
        if (urlContext == null) {
            urlContext = get(URL_CONTEXT);
            if (null == urlContext) {
                urlContext = "/";
            }
        }
        return urlContext;
    }
    private void _mergeUrlContext(AppConfig conf) {
        if (null == get(URL_CONTEXT)) {
            urlContext = conf.urlContext;
        }
    }

    private String xForwardedProtocol = null;
    protected T forceHttps() {
        xForwardedProtocol = "https";
        return me();
    }
    public String xForwardedProtocol() {
        if (null == xForwardedProtocol) {
            xForwardedProtocol = get(X_FORWARD_PROTOCOL);
            if (null == xForwardedProtocol) {
                xForwardedProtocol = "http";
            }
        }
        return xForwardedProtocol;
    }
    private void _mergeXForwardedProtocol(AppConfig conf) {
        if (null == get(X_FORWARD_PROTOCOL)) {
            xForwardedProtocol = conf.xForwardedProtocol;
        }
    }

    private String controllerPackage = null;
    protected T controllerPackage(String pkg) {
        pkg = pkg.trim();
        E.illegalArgumentIf(pkg.length() == 0, "package name cannot be empty");
        controllerPackage = pkg;
        return me();
    }
    public String controllerPackage() {
        if (null == controllerPackage) {
            controllerPackage = get(CONTROLLER_PACKAGE);
        }
        return controllerPackage;
    }
    private void _mergeControllerPackage(AppConfig conf) {
        if (null == get(CONTROLLER_PACKAGE)) {
            controllerPackage = conf.controllerPackage;
        }
    }

    private String host = null;
    protected T host(String hostname) {
        hostname = hostname.trim();
        E.illegalArgumentIf(hostname.length() == 0, "hostname cannot be empty");
        host = hostname;
        return me();
    }
    public String host() {
        if (null == host) {
            host = get(HOST);
            if (null == host) {
                logger.warn("host is not configured. Use localhost as hostname");
                host = "localhost";
            }
        }
        return host;
    }
    private void _mergeHost(AppConfig conf) {
        if (null == get(HOST)) {
            host = conf.host;
        }
    }

    private int httpMaxParams = -1;
    protected T httpMaxParams(int max) {
        E.illegalArgumentIf(max < 0, "max params cannot be negative number: %s", max);
        this.httpMaxParams = max;
        return me();
    }
    public int httpMaxParams() {
        if (-1 == httpMaxParams) {
            Integer I = get(HTTP_MAX_PARAMS);
            if (null == I) {
                I = 1000;
            }
            if (I < 0) {
                throw new ConfigurationException("http.params.max setting cannot be negative number. Found: %s", I);
            }
            httpMaxParams = I;
        }
        return httpMaxParams;
    }
    private void _mergeHttpMaxParams(AppConfig conf) {
        if (null == get(HTTP_MAX_PARAMS)) {
            httpMaxParams = conf.httpMaxParams;
        }
    }

    private int jobPoolSize = -1;
    protected T jobPoolSize(int size) {
        E.illegalArgumentIf(size < 1, "job pool size cannot be zero or negative number: %s", size);
        this.jobPoolSize = jobPoolSize;
        return me();
    }
    public int jobPoolSize() {
        if (-1 == jobPoolSize) {
            Integer I = get(JOB_POOL_SIZE);
            if (null == I) {
                I = 10;
            }
            jobPoolSize = I;
        }
        return jobPoolSize;
    }
    private void _mergeJobPoolSize(AppConfig conf) {
        if (null == get(JOB_POOL_SIZE)) {
            jobPoolSize = conf.jobPoolSize;
        }
    }

    private int port = -1;
    protected T port(int port) {
        E.illegalArgumentIf(port < 1, "port value not valid: %s", port);
        this.port = port;
        return me();
    }
    public int port() {
        if (-1 == port) {
            Integer I = get(PORT);
            if (null == I) {
                I = 5460;
            }
            port = I;
        }
        return port;
    }
    private void _mergePort(AppConfig conf) {
        if (null == get(PORT)) {
            port = conf.port;
        }
    }

    private String encoding = null;
    protected T encoding(String encoding) {
        encoding = encoding.trim();
        E.illegalArgumentIf(encoding.length() == 0, "encoding cannot be empty");
        this.encoding = encoding;
        return me();
    }
    public String encoding() {
        if (null == encoding) {
            encoding = get(ENCODING);
            if (null == encoding) {
                encoding = Charsets.UTF_8.name().toLowerCase();
            }
        }
        return encoding;
    }
    private void _mergeEncoding(AppConfig conf) {
        if (null == get(ENCODING)) {
            encoding = conf.encoding;
        }
    }

    private String dateFmt = null;
    protected T dateFormat(String fmt) {
        E.illegalArgumentIf(S.blank(fmt), "Date format cannot be empty");
        this.dateFmt = fmt;
        return me();
    }
    public String dateFormat() {
        if (null == dateFmt) {
            dateFmt = get(FORMAT_DATE);
            if (null == dateFmt) {
                DateFormat formatter = DateFormat.getDateInstance();
                dateFmt  = ((SimpleDateFormat)formatter).toLocalizedPattern();
            }
        }
        return dateFmt;
    }
    private void _mergeDateFmt(AppConfig conf) {
        if (null == get(FORMAT_DATE)) {
            dateFmt = conf.dateFmt;
        }
    }

    private String timeFmt = null;
    protected T timeFormat(String fmt) {
        E.illegalArgumentIf(S.blank(fmt), "Time format cannot be empty");
        this.timeFmt = fmt;
        return me();
    }
    public String timeFormat() {
        if (null == timeFmt) {
            timeFmt = get(FORMAT_TIME);
            if (null == timeFmt) {
                DateFormat formatter = DateFormat.getTimeInstance();
                timeFmt  = ((SimpleDateFormat)formatter).toLocalizedPattern();
            }
        }
        return timeFmt;
    }
    private void _mergeTimeFmt(AppConfig conf) {
        if (null == get(FORMAT_TIME)) {
            timeFmt = conf.timeFmt;
        }
    }

    private String dateDateTimeFmt = null;
    protected T dateTimeFormat(String fmt) {
        E.illegalArgumentIf(S.blank(fmt), "Date time format cannot be empty");
        this.dateDateTimeFmt = fmt;
        return me();
    }
    public String dateTimeFormat() {
        if (null == dateDateTimeFmt) {
            dateDateTimeFmt = get(FORMAT_TIME);
            if (null == dateDateTimeFmt) {
                DateFormat formatter = DateFormat.getDateInstance();
                dateDateTimeFmt  = ((SimpleDateFormat)formatter).toLocalizedPattern();
            }
        }
        return dateDateTimeFmt;
    }
    private void _mergeDateTimeFmt(AppConfig conf) {
        if (null == get(FORMAT_TIME)) {
            dateDateTimeFmt = conf.dateDateTimeFmt;
        }
    }

    private Locale locale = null;
    protected T locale(Locale locale) {
        E.NPE(locale);
        this.locale = locale;
        return me();
    }
    public Locale locale() {
        if (null == locale) {
            locale = get(LOCALE);
            if (null == locale) {
                locale = Locale.getDefault();
            }
        }
        return locale;
    }
    private void _mergeLocale(AppConfig conf) {
        if (null == get(LOCALE)) {
            locale = conf.locale;
        }
    }

    private String sourceVersion = null;
    protected T sourceVersion(JavaVersion version) {
        sourceVersion = FastStr.of(version.name()).substr(1).replace('_', '.').toString();
        return me();
    }
    public String sourceVersion() {
        if (null == sourceVersion) {
            sourceVersion = get(AppConfigKey.SOURCE_VERSION);
            if (null == sourceVersion) {
                sourceVersion = "1." + _.JAVA_VERSION;
            }
        }
        return sourceVersion;
    }
    private void _mergeSourceVersion(AppConfig conf) {
        if (null == get(SOURCE_VERSION)) {
            sourceVersion = conf.sourceVersion;
        }
    }

    private String targetVersion = null;
    protected T targetVersion(JavaVersion version) {
        targetVersion = FastStr.of(version.name()).substr(1).replace('_', '.').toString();
        return me();
    }
    public String targetVersion() {
        if (null == targetVersion) {
            targetVersion = get(TARGET_VERSION);
            if (null == targetVersion) {
                targetVersion = "1." + _.JAVA_VERSION;
            }
        }
        return targetVersion;
    }
    private void _mergeTargetVersion(AppConfig conf) {
        if (null == get(TARGET_VERSION)) {
            targetVersion = conf.targetVersion;
        }
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
            String controllerPackage = get(CONTROLLER_PACKAGE);
            if (S.isBlank(controllerPackage)) {
                _.Predicate<String> f = _.F.no();
                CONTROLLER_CLASS_TESTER = f.or(app().router().f.IS_CONTROLLER);
            } else {
                final String cp = controllerPackage.trim();
                _.Predicate<String> f = S.F.startsWith(cp);
                CONTROLLER_CLASS_TESTER = f.or(app().router().f.IS_CONTROLLER);
            }
        }
        return CONTROLLER_CLASS_TESTER;
    }

    private TemplatePathResolver templatePathResolver = null;
    protected T templatePathResolver(TemplatePathResolver resolver) {
        E.NPE(resolver);
        templatePathResolver = resolver;
        return me();
    }
    public TemplatePathResolver templatePathResolver() {
        if (null == templatePathResolver) {
            templatePathResolver = get(AppConfigKey.TEMPLATE_PATH_RESOLVER);
            if (null == templatePathResolver) {
                templatePathResolver = new TemplatePathResolver();
            }
        }
        return templatePathResolver;
    }
    private void _mergeTemplatePathResolver(AppConfig conf) {
        if (null == get(AppConfigKey.TEMPLATE_PATH_RESOLVER)) {
            templatePathResolver = conf.templatePathResolver;
        }
    }

    private String templateHome = null;
    protected T templateHome(String home) {
        home = home.trim();
        E.illegalArgumentIf(home.length() == 0, "template home cannot be empty");
        templateHome = home;
        return me();
    }
    public String templateHome() {
        if (null == templateHome) {
            templateHome = get(AppConfigKey.TEMPLATE_HOME);
            if (null == templateHome) {
                templateHome = "default";
            }
        }
        return templateHome;
    }
    private void _mergeTemplateHome(AppConfig conf) {
        if (null == get(AppConfigKey.TEMPLATE_HOME)) {
            templateHome = conf.templateHome;
        }
    }

    private String defViewName = null;
    private View defView = null;
    protected T defaultView(View view) {
        E.NPE(view);
        defView = view;
        return me();
    }
    public View defaultView() {
        if (null == defViewName) {
            defViewName = get(AppConfigKey.VIEW_DEFAULT);
            if (null == defViewName) {
                defViewName = "rythm";
            }
            defView = Act.viewManager().view(defViewName);
        }
        return defView;
    }
    private void _mergeDefaultView(AppConfig conf) {
        if (null == get(AppConfigKey.VIEW_DEFAULT)) {
            defViewName = conf.defViewName;
            defView = conf.defView;
        }
    }

    private boolean pingPathResolved = false;
    private String pingPath = null;
    protected T pingPath(String path) {
        pingPathResolved = true;
        pingPath = path.trim();
        return me();
    }
    public String pingPath() {
        if (!pingPathResolved) {
            pingPath = get(AppConfigKey.PING_PATH);
            pingPathResolved = true;
        }
        return pingPath;
    }

    private void _mergePingPath(AppConfig config) {
        if (null == get(AppConfigKey.PING_PATH)) {
            pingPath = config.pingPath;
            pingPathResolved = config.pingPathResolved;
        }
    }

    private String sessionCookieName = null;
    protected T sessionCookieName(String name) {
        name = name.trim().toLowerCase();
        E.illegalArgumentIf(name.length() == 0, "session cookie name cannot be blank");
        E.illegalArgumentIf(S.eq(name, flashCookieName), "session cookie name cannot be the same with flash cookie name");
        sessionCookieName = name;
        return me();
    }
    public String sessionCookieName() {
        if (null == sessionCookieName) {
            String sessionCookiePrefix = get(AppConfigKey.SESSION_PREFIX);
            sessionCookieName = S.builder(sessionCookiePrefix).append("_").append("session").toString();
        }
        return sessionCookieName;
    }
    private void _mergeSessionCookieName(AppConfig config) {
        if (null != config.sessionCookieName) {
            sessionCookieName = config.sessionCookieName;
        }
    }

    private String flashCookieName = null;
    protected T flashCookieName(String name) {
        name = name.trim().toLowerCase();
        E.illegalArgumentIf(name.length() == 0, "flash cookie name cannot be blank");
        E.illegalArgumentIf(S.eq(name, sessionCookieName), "flash cookie name cannot be the same with session cookie name");
        flashCookieName = name;
        return me();
    }
    public String flashCookieName() {
        if (null == flashCookieName) {
            String sessionCookiePrefix = get(AppConfigKey.SESSION_PREFIX);
            flashCookieName = S.builder(sessionCookiePrefix).append("_").append("flash").toString();
        }
        return flashCookieName;
    }
    private void _mergeFlashCookieName(AppConfig config) {
        if (null != config.flashCookieName) {
            flashCookieName = config.flashCookieName;
        }
    }

    private Long sessionTtl = null;
    protected T sessionTtl(long seconds) {
        sessionTtl = seconds;
        return me();
    }
    public long sessionTtl() {
        if (null == sessionTtl) {
            sessionTtl = get(AppConfigKey.SESSION_TTL);
            if (null == sessionTtl) {
                sessionTtl = (long) 60 * 30;
            }
        }
        return sessionTtl;
    }
    private void _mergeSessionTtl(AppConfig conf) {
        if (null == get(AppConfigKey.SESSION_TTL)) {
            sessionTtl = conf.sessionTtl;
        }
    }

    private Boolean sessionPersistent = null;
    protected T sessionPersistent(boolean persistenSession) {
        sessionPersistent = persistenSession;
        return me();
    }
    public boolean persistSession() {
        if (null == sessionPersistent) {
            sessionPersistent = get(AppConfigKey.SESSION_PERSISTENT_ENABLED);
            if (null == sessionPersistent) {
                sessionPersistent = false;
            }
        }
        return sessionPersistent;
    }
    private void _mergeSessionPersistent(AppConfig config) {
        if (null == get(AppConfigKey.SESSION_PERSISTENT_ENABLED)) {
            sessionPersistent = config.sessionPersistent;
        }
    }

    private Boolean sessionEncrypt = null;
    protected T sessionEncrypt(boolean encryptSession) {
        sessionEncrypt = encryptSession;
        return me();
    }
    public boolean encryptSession() {
        if (null == sessionEncrypt) {
            sessionEncrypt = get(AppConfigKey.SESSION_ENCRYPT_ENABLED);
            if (null == sessionEncrypt) {
                sessionEncrypt = false;
            }
        }
        return sessionEncrypt;
    }
    private void _mergeSessionEncrpt(AppConfig config) {
        if (null == get(AppConfigKey.SESSION_ENCRYPT_ENABLED)) {
            sessionEncrypt = config.sessionEncrypt;
        }
    }

    private Boolean sessionHttpOnly = null;
    protected T sessionHttpOnly(boolean httpOnly) {
        sessionHttpOnly = httpOnly;
        return me();
    }
    public boolean sessionHttpOnly() {
        if (null == sessionHttpOnly) {
            sessionHttpOnly = get(AppConfigKey.SESSION_HTTP_ONLY_ENABLED);
            if (null == sessionHttpOnly) {
                sessionHttpOnly = true;
            }
        }
        return sessionHttpOnly;
    }
    private void _mergeSessionHttpOnly(AppConfig config) {
        if (null == get(AppConfigKey.SESSION_HTTP_ONLY_ENABLED)) {
            sessionHttpOnly = config.sessionHttpOnly;
        }
    }

    private Boolean sessionSecure = null;
    protected T sessionSecure(boolean secure) {
        sessionSecure = secure;
        return me();
    }
    public boolean sessionSecure() {
        if (Act.isDev()) {
            return false;
        }
        if (null == sessionSecure) {
            sessionSecure = get(AppConfigKey.SESSION_SECURE);
            if (null == sessionSecure) {
                sessionSecure = true;
            }
        }
        return sessionSecure;
    }
    private void _mergeSessionSecure(AppConfig config) {
        if (null == get(AppConfigKey.SESSION_SECURE)) {
            sessionSecure = config.sessionSecure;
        }
    }

    private String secret = null;
    protected T secret(String secret) {
        E.illegalArgumentIf(S.blank(secret));
        this.secret = secret;
        return me();
    }
    public String secret() {
        if (null == secret) {
            secret = get(AppConfigKey.SECRET);
            if (null == secret) {
                secret ="myawesomeapp";
                logger.warn("Application secret key not set! You are in the dangerous zone!!!");
            }
        }
        return secret;
    }
    private void _mergeSecret(AppConfig config) {
        if (null == get(AppConfigKey.SECRET)) {
            secret = config.secret;
        }
    }

    public boolean possibleControllerClass(String className) {
        return controllerNameTester().test(className);
    }

    private CacheServiceProvider csp = null;
    protected T cacheService(CacheServiceProvider csp) {
        E.NPE(csp);
        this.csp = csp;
        return me();
    }
    protected T cacheService(Class<? extends CacheServiceProvider> csp) {
        this.csp = _.newInstance(csp);
        return me();
    }
    public CacheService cacheService(String name) {
        if (null == csp) {
            csp = get(AppConfigKey.CACHE_IMPL);
            if (null == csp) {
                csp = CacheServiceProvider.Impl.Simple;
            }
        }
        return csp.get(name);
    }
    private void _mergeCacheServiceProvider(AppConfig config) {
        if (null == get(AppConfigKey.CACHE_IMPL)) {
            csp = config.csp;
        }
    }

    private boolean _merged = false;
    /**
     * Merge application configurator settings. Note application configurator
     * settings has lower priority as it's hardcoded thus only when configuration file
     * does not provided the settings, the app configurator will take effect
     * @param conf the application configurator
     */
    public void _merge(AppConfigurator conf) {
        if (_merged) {
            return;
        }
        _merged = true;
        _mergeUrlContext(conf);
        _mergeXForwardedProtocol(conf);
        _mergeControllerPackage(conf);
        _mergeHost(conf);
        _mergeHttpMaxParams(conf);
        _mergeJobPoolSize(conf);
        _mergePort(conf);
        _mergeDateFmt(conf);
        _mergeDateTimeFmt(conf);
        _mergeTimeFmt(conf);
        _mergeEncoding(conf);
        _mergeLocale(conf);
        _mergeSourceVersion(conf);
        _mergeTargetVersion(conf);
        _mergeTemplatePathResolver(conf);
        _mergeTemplateHome(conf);
        _mergeDefaultView(conf);
        _mergeSessionCookieName(conf);
        _mergeFlashCookieName(conf);
        _mergeSessionTtl(conf);
        _mergeSessionPersistent(conf);
        _mergeSessionEncrpt(conf);
        _mergeSessionHttpOnly(conf);
        _mergeSessionSecure(conf);
        _mergeSecret(conf);
        _mergeCacheServiceProvider(conf);

        Set<String> keys = conf.propKeys();
        for (String k : keys) {
            if (!raw.containsKey(k)) {
                raw.put(k, conf.propVal(k));
            }
        }
    }

    @Override
    protected void releaseResources() {
        app = null;
        super.releaseResources();
    }

    protected T me() {
        return _.cast(this);
    }

}
