package act.app;

import act.Act;
import org.osgl._;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.Iterator;
import java.util.Map;

/**
 * Manage applications deployed on Act
 */
public class AppManager {

    private Map<Integer, App> byPort = C.newMap();
    private Map<String, App> byContextPath = C.newMap();

    private AppManager() {
    }

    public AppManager scan() {
        Act.mode().appScanner().scan(_F.loadApp(this));
        return this;
    }

    public AppManager loadSingleApp() {
        AppScanner.SINGLE_APP_SCANNER.scan(_F.loadApp(this));
        return this;
    }

    public void deploy(App app) {
        load(app);
        refresh(app);
    }

    public void refresh() {
        Iterator<App> itr = appIterator();
        while (itr.hasNext()) {
            refresh(itr.next());
        }
    }

    public void refresh(App app) {
        app.refresh();
    }

    public void load(App app) {
        app.build();
        app.refresh();
        int port = app.config().port();
        if (port < 0) {
            loadIntoContextMap(app.config().urlContext(), app);
        } else {
            loadIntoPortMap(port, app);
        }
        app.hook();
    }

    private void loadIntoPortMap(int port, App app) {
        App app0 = byPort.get(port);
        if (null != app0) {
            E.invalidConfigurationIf(!app.equals(app0), "Another application has already been deployed using port %s", port);
        } else {
            byPort.put(port, app);
        }
    }

    private void loadIntoContextMap(String context, App app) {
        App app0 = byContextPath.get(context);
        if (null != app0) {
            E.invalidConfigurationIf(!app.equals(app0), "Another application has already been deployed using context %s", context);
        } else {
            byContextPath.put(context, app);
        }
    }

    private Iterator<App> appIterator() {
        final Iterator<App> itrByPort = byPort.values().iterator();
        final Iterator<App> itrByContext = byContextPath.values().iterator();
        return new Iterator<App>() {
            boolean byPortFinished = !itrByPort.hasNext();

            @Override
            public boolean hasNext() {
                if (!byPortFinished) {
                    byPortFinished = !itrByPort.hasNext();
                }
                return !byPortFinished || itrByContext.hasNext();
            }

            @Override
            public App next() {
                return byPortFinished ? itrByContext.next() : itrByPort.next();
            }

            @Override
            public void remove() {
                E.unsupport();
            }
        };
    }

    public static AppManager create() {
        return new AppManager();
    }

    private enum _F {
        ;

        static final _.F1<App, ?> loadApp(final AppManager mgr) {
            return new _.Visitor<App>() {
                @Override
                public void visit(App app) throws _.Break {
                    mgr.load(app);
                }
            };
        }
    }
}
