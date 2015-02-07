package org.osgl.oms.conf;

import org.osgl.oms.app.ProjectLayout;

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

    private ProjectLayout projectLayout = null;
    public ProjectLayout projectLayout() {
        if (null == projectLayout) {
            projectLayout = get(PROJECT_LAYOUT);
        }
        return projectLayout;
    }

}
