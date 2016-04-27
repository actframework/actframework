package act.app;

import act.util.SubTypeFinder;

/**
 * Find classes that implement {@link Daemon}, start them after {@link App application} is started
 */
public class DaemonFinder extends SubTypeFinder<Daemon> {
    public DaemonFinder(Class<Daemon> target) {
        super(target);
    }

    @Override
    protected void found(final Class<Daemon> target, final App app) {
        app.registerDaemon(app.newInstance(target));
    }
}
