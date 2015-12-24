package act.app;

import act.cli.Command;

/**
 * Admin interface to {@link App}
 */
public class AppAdmin {

    @Command("act.app.restart")
    public void restart(CliContext context) {
        context.println("About to restart app. Your telnet session will be terminated. Please reconnect in a few seconds ...");
        context.app().restart();
    }

}
