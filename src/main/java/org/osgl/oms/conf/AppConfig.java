package org.osgl.oms.conf;

import org.osgl._;
import org.osgl.cache.CacheService;
import org.osgl.cache.CacheServiceProvider;
import org.osgl.oms.Constants;
import org.osgl.oms.OMS;
import org.osgl.oms.view.TemplatePathResolver;
import org.osgl.oms.view.View;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.Locale;
import java.util.Map;

import static org.osgl.oms.conf.AppConfigKey.*;

public class AppConfig extends Config<AppConfigKey> {

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
            urlContext = get(URL_CONTEXT);
            if (null == urlContext) {
                urlContext = "/";
            }
        }
        return urlContext;
    }

    private String xForwardedProtocol = null;

    public String xForwardedProtocol() {
        if (null == xForwardedProtocol) {
            xForwardedProtocol = get(X_FORWARD_PROTOCOL);
        }
        return xForwardedProtocol;
    }

    private String controllerPackage = null;

    public String controllerPackage() {
        if (null == controllerPackage) {
            controllerPackage = get(CONTROLLER_PACKAGE);
        }
        return controllerPackage;
    }

    private String host = null;

    public String host() {
        if (null == host) {
            host = get(HOST);
        }
        return host;
    }

    private int port = -1;

    public int port() {
        if (-1 == port) {
            port = get(PORT);
        }
        return port;
    }

    private String encoding = null;

    public String encoding() {
        if (null == encoding) {
            encoding = get(ENCODING);
        }
        return encoding;
    }

    private Locale locale = null;

    public Locale locale() {
        if (null == locale) {
            locale = get(LOCALE);
        }
        return locale;
    }

    private String sourceVersion = null;

    public String sourceVersion() {
        if (null == sourceVersion) {
            sourceVersion = get(SOURCE_VERSION);
        }
        return sourceVersion;
    }

    private _.Predicate<String> APP_CLASS_TESTER = null;

    private _.Predicate<String> appClassTester() {
        if (null == APP_CLASS_TESTER) {
            String scanPackage = get(SCAN_PACKAGE);
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
            templatePathResolver = get(TEMPLATE_PATH_RESOLVER);
        }
        return templatePathResolver;
    }

    private String templateHome = null;

    public String templateHome() {
        if (null == templateHome) {
            templateHome = get(TEMPLATE_HOME);
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
            defViewName = get(VIEW_DEFAULT);
            defView = OMS.viewManager().view(defViewName);
        }
        return defView;
    }

    public boolean notControllerClass(String className) {
        return !controllerNameTester().test(className);
    }

    private CacheServiceProvider csp = null;

    public CacheService cacheService(String name) {
        throw E.tbd();
    }
}
