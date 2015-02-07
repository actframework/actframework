package org.osgl.oms;

import org.osgl.oms.cls.BootstrapClassLoader;

import java.lang.reflect.Method;

/**
 * bootstrap the OMS
 */
public class Bootstrap {
    public static void main(String[] args) throws Exception {
        BootstrapClassLoader classLoader = new BootstrapClassLoader(Bootstrap.class.getClassLoader());
        Class<?> omsClass = classLoader.loadClass("org.osgl.oms.OMS");
        Method m = omsClass.getDeclaredMethod("start");
        m.invoke(null);
    }
}
