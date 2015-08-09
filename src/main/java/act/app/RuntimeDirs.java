package act.app;

import act.Act;
import act.route.RouteTableRouterBuilder;

import java.io.File;

/**
 * Define application dir structure at runtime
 */
public enum RuntimeDirs {
    ;

    public static final String CONF = "/conf";
    public static final String ASSET = "/asset";
    public static final String CLASSES = "/classes";
    public static final String LIB = "/lib";

    public static File home(App app) {
        if (Act.isDev()) {
            return app.layout().target(app.base());
        } else {
            return app.base();
        }
    }

    public static File conf(App app) {
        return new File(Act.isDev() ? new File(app.base(), "src/main/webapp") : app.home(), CONF);
    }

    public static File routes(App app) {
        return new File(classes(app), RouteTableRouterBuilder.ROUTES_FILE);
    }

    public static File asset(App app) {
        return new File(app.home(), ASSET);
    }

    public static File classes(App app) {
        return new File(app.home(), CLASSES);
    }

    public static File lib(App app) {
        return new File(app.home(), LIB);
    }

}
