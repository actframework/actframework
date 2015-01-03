package org.osgl.mvc.server;

public class AppConfig {
    private String urlContext = "";
    private String xForwardedProtocol = "http";
    public String urlContext() {
        return urlContext;
    }
    public String xForwardedProtocol() {
        return xForwardedProtocol;
    }
}
