package act.app;

import act.cli.Command;
import act.cli.Required;
import act.di.Context;
import act.util.PropertySpec;
import org.osgl.util.E;

/**
 * Access application daemon status
 */
@SuppressWarnings("unused")
public class DaemonAdmin {

    @Command(name = "act.daemon.list", help = "List app daemons")
    @PropertySpec("id,state")
    public Iterable<Daemon> list() {
        return App.instance().registeredDaemons();
    }

    @Command(name = "act.daemon.start", help = "Start app daemon")
    public void start(
            @Required("specify daemon id") String id,
            @Context CliContext context
    ) {
        Daemon daemon = get(id, context);
        daemon.start();
        report(daemon, context);
    }

    @Command(name = "act.daemon.stop", help = "Stop app daemon")
    public void stop(
            @Required("specify daemon id") String id,
            @Context CliContext context
    ) {
        Daemon daemon = get(id, context);
        daemon.stop();
        report(daemon, context);
    }

    @Command(name = "act.daemon.restart", help = "Re-Start app daemon")
    public void restart(
            @Required("specify daemon id") String id,
            @Context CliContext context
    ) {
        Daemon daemon = get(id, context);
        daemon.restart();
        report(daemon, context);
    }

    @Command(name = "act.daemon.status", help = "Report app daemon status")
    public void status(
            @Required("specify daemon id") String id,
            @Context CliContext context
    ) {
        Daemon daemon = get(id, context);
        Daemon.State state = daemon.state();
        Exception lastError = daemon.lastError();
        context.println("Daemon[%s]: %s", id, state);
        if (null != lastError) {
            context.println("Last error: %s", E.stackTrace(lastError));
        }
    }

    private static Daemon get(String id, CliContext context) {
        Daemon daemon = App.instance().registeredDaemon(id);
        if (null == daemon) {
            context.println("Unknown daemon: %s", id);
            return null;
        }
        return daemon;
    }

    private static void report(Daemon daemon, CliContext context) {
        context.println("Daemon[%s]: %s", daemon.id(), daemon.state());
    }
}
