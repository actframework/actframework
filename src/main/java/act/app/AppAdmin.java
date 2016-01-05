package act.app;

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

}
