package act.cli;

import act.app.App;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;
import org.osgl.util.C;
import org.osgl.util.S;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * Allow user to use TAB to auto complete command name
 */
@Singleton
public class CommandNameCompleter extends StringsCompleter implements Completer {

    @Inject
    public CommandNameCompleter(App app) {
        super(allCommandNames(app.cliDispatcher()));
    }

    private static List<String> allCommandNames(CliDispatcher dispatcher) {
        List<String> l = C.newList();

        List<String> appCommands = dispatcher.commands(false, true);
        l.addAll(appCommands);

        List<String> sysCommands = dispatcher.commands(true, false);
        l.addAll(sysCommands);

        for (String sysCmd : sysCommands) {
            l.add(S.afterFirst(sysCmd, "act."));
        }
        return l;
    }
}
