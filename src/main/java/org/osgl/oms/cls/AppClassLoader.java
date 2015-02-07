package org.osgl.oms.cls;

import org.osgl.oms.app.ProjectLayout;
import org.osgl.oms.conf.AppConfig;
import org.osgl.util.E;

import java.io.File;

/**
 * The top level class loader to load a specific application classes into JVM
 */
public class AppClassLoader extends ClassLoader {
    private File appRoot;
    private AppConfig config;
    private ProjectLayout layout;

    public AppClassLoader(File appBase, AppConfig config, ProjectLayout layout) {
        E.NPE(appBase, config, layout);
        this.appRoot = appBase;
        this.config = config;
        this.layout = layout;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }

    public void scan() {

    }
}
