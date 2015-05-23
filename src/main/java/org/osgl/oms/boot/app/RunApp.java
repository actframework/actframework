package org.osgl.oms.boot.app;

import org.osgl.oms.conf.AppConfigKey;

import java.lang.reflect.Method;

/**
 * The entry to start an OMS full stack app
 */
public class RunApp {

    public static void start(Class<?> anyController) throws Exception {
        String pkg = anyController.getPackage().getName();
        System.setProperty(AppConfigKey.CONTROLLER_PACKAGE.key(), pkg);
        FullStackAppBootstrapClassLoader classLoader = new FullStackAppBootstrapClassLoader(RunApp.class.getClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
        Class<?> omsClass = classLoader.loadClass("org.osgl.oms.OMS");
        Method m = omsClass.getDeclaredMethod("startApp");
        m.invoke(null);
    }
}
