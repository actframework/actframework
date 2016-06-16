package act.app;

import act.Act;
import act.cli.Command;

/**
 * Admin interface to {@link App}
 */
public class AppAdmin {

    @Command(value = "act.app.restart", help = "restart application node")
    public void restart(CliContext context) {
        context.println("About to restart app. Your telnet session will be terminated. Please reconnect in a few seconds ...");
        context.flush();
        context.app().restart();
    }

    @Command(value = "act.app.shutdown", help = "shutdown application node")
    public void shutdown(CliContext context) {
        context.println("About to shutdown app. Your telnet session will be terminated. Please reconnect in a few seconds ...");
        context.flush();
        Act.shutdownApp(context.app());
    }

}
