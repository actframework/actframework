package org.osgl.mvc.server;

import java.util.Properties;

public class AppConfig {
    private String urlContext = "";
    private String xForwardedProtocol = "http";
    private String controllerPackage = "";
    private Properties p;

    public AppConfig() {
        this(System.getProperties());
    }

    public AppConfig(Properties p) {
        if (p.containsKey("urlContext")) {
            urlContext = p.getProperty("urlContext");
        }
        if (p.containsKey("controllerPackage")) {
            controllerPackage = p.getProperty("controllerPackage");
        }
        this.p = p;
    }

    public String urlContext() {
        return urlContext;
    }
    public String xForwardedProtocol() {
        return xForwardedProtocol;
    }
    public String controllerPackage() {
        return controllerPackage;
    }
}
