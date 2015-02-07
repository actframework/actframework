package org.osgl.oms.app;

import org.osgl._;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.oms.cls.AppClassLoader;
import org.osgl.oms.conf.AppConfLoader;
import org.osgl.oms.conf.AppConfig;
import org.osgl.oms.route.RouteTableRouterBuilder;
import org.osgl.oms.route.Router;
import org.osgl.util.IO;

import java.io.File;
import java.util.List;

/**
 * {@code App} represents an application that is deployed in a OMS container
 */
public class App {

    private static Logger logger = L.get(App.class);

    private File appBase;
    private Router router;
    private AppConfig config;
    private AppClassLoader classLoader;
    private ProjectLayout layout;

    private App(File appBase, ProjectLayout layout) {
        this.appBase = appBase;
        this.layout = layout;
    }

    public AppConfig config() {
        return config;
    }

    public Router router() {
        return router;
    }

    public File base() {
        return appBase;
    }

    public ProjectLayout layout() {
        return layout;
    }

    public void refresh() {
    }

    @Override
    public int hashCode() {
        return appBase.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof App) {
            App that = (App) obj;
            return _.eq(that.appBase, appBase);
        }
        return false;
    }

    @Override
    public String toString() {
        return appBase.getName();
    }

    private void loadConfig(File conf) {
        logger.debug("loading app configuration: %s ...", appBase.getPath());
        config = new AppConfLoader().load(conf);
    }

    private void loadRoutes(File routes) {
        logger.debug("loading app routing table: %s ...", appBase.getPath());
        router = new Router(config);
        if (!(routes.isFile() && routes.canRead())) {
            return;
        }
        List<String> lines = IO.readLines(routes);
        new RouteTableRouterBuilder(lines).build(router);
    }

    private void loadClasses() {
        classLoader = new AppClassLoader(appBase, config, layout);
    }

    static App create(File appBase, ProjectLayout layout) {
        return new App(appBase, layout);
    }
}
