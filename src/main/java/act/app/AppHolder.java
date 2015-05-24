package act.app;

import org.osgl.util.E;

/**
 * Base class for application aspect managers
 */
public abstract class AppHolder {

    private App app;

    protected AppHolder(App app) {
        E.NPE(app);
        this.app = app;
    }

    protected App app() {
        return app;
    }
}
