package org.osgl.oms.boot.server;

import java.lang.reflect.Method;

/**
 * bootstrap the OMS server instance
 */
public class RunServer {
    public static void main(String[] args) throws Exception {
        ServerBootstrapClassLoader classLoader = new ServerBootstrapClassLoader(RunServer.class.getClassLoader());
        Class<?> omsClass = classLoader.loadClass("org.osgl.oms.OMS");
        Method m = omsClass.getDeclaredMethod("startServer");
        m.invoke(null);
    }
}
