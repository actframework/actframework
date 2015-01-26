package org.osgl.mvc.server.plugin;

public interface ActionMethodInfoSource {
    boolean isActionMethod(String className, String methodName);
}
