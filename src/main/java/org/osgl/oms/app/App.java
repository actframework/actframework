package org.osgl.oms.app;

import org.osgl._;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.oms.OMS;
import org.osgl.oms.conf.AppConfLoader;
import org.osgl.oms.conf.AppConfig;
import org.osgl.oms.route.RouteTableRouterBuilder;
import org.osgl.oms.route.Router;
import org.osgl.util.E;
import org.osgl.util.IO;

import java.io.File;
import java.util.List;

/**
 * {@code App} represents an application that is deployed in a OMS container
 */
public class App {

    private static Logger logger = L.get(App.class);

    /**
     * The base dir where an application sit within
     */
    private File appBase;
    /**
     * The home dir of an application, referenced only
     * at runtime.
     * <p><b>Note</b> when app is running in dev mode, {@code appHome}
     * shall be {@code appBase/target}, while app is deployed to
     * OMS at other mode, {@code appHome} shall be the same as
     * {@code appBase}</p>
     */
    private File appHome;
    private Router router;
    private AppConfig config;
    private AppClassLoader classLoader;
    private ProjectLayout layout;
    private AppBuilder builder;

    private App(File appBase, ProjectLayout layout) {
        this.appBase = appBase;
        this.layout = layout;
        this.appHome = RuntimeDirs.home(this);
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

    public File home() {
        return appHome;
    }

    public AppClassLoader classLoader() {
        return classLoader;
    }

    public ProjectLayout layout() {
        return layout;
    }

    void refresh() {
        loadConfig();
        initRouter();
        loadClasses();
        loadRoutes();
    }

    void build() {
        builder = AppBuilder.build(this);
    }

    void hook() {
        OMS.hook(this);
    }

    public AppBuilder builder() {
        return builder;
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

    private void loadConfig() {
        File conf = RuntimeDirs.conf(this);
        logger.debug("loading app configuration: %s ...", appBase.getPath());
        config = new AppConfLoader().load(conf);
    }

    private void initRouter() {
        router = new Router(this);
    }

    private void loadRoutes() {
        logger.debug("loading app routing table: %s ...", appBase.getPath());
        File routes = RuntimeDirs.routes(this);
        if (!(routes.isFile() && routes.canRead())) {
            logger.warn("Cannot find routes file: %s", appBase.getPath());
            // guess the app is purely using annotation based routes
            return;
        }
        List<String> lines = IO.readLines(routes);
        new RouteTableRouterBuilder(lines).build(router);
    }

    private void loadClasses() {
        classLoader = OMS.mode().classLoader(this);
    }

    static App create(File appBase, ProjectLayout layout) {
        return new App(appBase, layout);
    }
}
