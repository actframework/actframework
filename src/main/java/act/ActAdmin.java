package act;

import act.app.App;
import act.cli.CliContext;
import act.cli.Command;

/**
 * Admin interface to {@link App}
 */
public class ActAdmin {

    @Command(value = "act.restart", help = "restart application node")
    public void restart(CliContext context) {
        context.println("About to restart app. Your telnet session will be terminated. Please reconnect in a few seconds ...");
        context.flush();
        context.app().restart();
    }

    @Command(value = "act.shutdown", help = "shutdown application node")
    public void shutdown(CliContext context) {
        context.println("About to shutdown app. Your telnet session will be terminated. Please reconnect in a few seconds ...");
        context.flush();
        Act.shutdownApp(context.app());
    }

}
