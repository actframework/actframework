package org.osgl.oms.conf;

import org.osgl._;
import org.osgl.util.S;

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
        this((Map)System.getProperties());
    }

    @Override
    protected ConfigKey keyOf(String s) {
        return AppConfigKey.valueOfIgnoreCase(s);
    }

    private String urlContext = null;
    public String urlContext() {
        if (urlContext == null) {
            urlContext = get(URL_CONTEXT);
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

    private int port = -1;
    public int port() {
        if (-1 == port) {
            port = get(PORT);
        }
        return port;
    }

    private String sourceVersion = null;
    public String sourceVersion() {
        if (null == sourceVersion) {
            sourceVersion = get(SOURCE_VERSION);
        }
        return sourceVersion;
    }

    private _.Predicate<String> APP_CLASS_TESTER = null;
    public boolean needEnhancement(String className) {
        if (null == APP_CLASS_TESTER) {
            String scanPackage = get(SCAN_PACKAGE);
            if (S.isBlank(scanPackage)) {
                APP_CLASS_TESTER = _.F.yes();
            } else {
                final String sp = scanPackage.trim();
                APP_CLASS_TESTER = new _.Predicate<String>(){
                    @Override
                    public boolean test(String s) {
                        return s.startsWith(sp);
                    }
                };
            }
        }
        return APP_CLASS_TESTER.test(className);
    }
}
