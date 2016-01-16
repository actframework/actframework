package act.cli.builtin;

import act.app.CliContext;
import act.cli.CliDispatcher;
import act.cli.util.CommandLineParser;
import act.handler.CliHandler;
import act.handler.CliHandlerBase;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;

import static org.osgl.Osgl.T2;

public class Help extends CliHandlerBase {

    public static final Help INSTANCE = new Help();

    private static int maxWidth = 0;

    private Help() {}

    @Override
    public void handle(CliContext context) {
        List<String> args = context.arguments();
        String command = null;
        if (args.size() > 0) {
            command = args.get(0);
            if (showHelp(command, context)) {
                return;
            }
        }
        CommandLineParser parser = context.commandLine();
        boolean sys = parser.getBoolean("-s", "--system");
        boolean app = parser.getBoolean("-a", "--app");
        if (!sys && !app) {
            sys = true;
            app = true;
        }

        CliDispatcher dispatcher = context.app().cliDispatcher();
        List<String> sysCommands = dispatcher.commands(true, false);
        List<String> appCommands = dispatcher.commands(false, true);
        int maxLen;
        if (sys && !app) {
            maxLen = calMaxCmdLen(sysCommands);
        } else if (app && !sys) {
            maxLen = calMaxCmdLen(appCommands);
        } else {
            maxLen = calMaxCmdLen(C.list(sysCommands).append(appCommands));
        }
        String fmt = "%-" + (maxLen + 4) + "s - %s";
        if (sys) {
            list(command, "System commands", fmt, sysCommands, dispatcher, context);
        }
        if (app) {
            if (sys) {
                context.println("");
            }
            list(command, "Application commands", fmt, appCommands, dispatcher, context);
        }
    }

    private void list(String search, String label, String fmt, List<String> commands, CliDispatcher dispatcher, CliContext context) {
        List<String> lines = C.newList();
        boolean noSearch = S.blank(search);
        if (noSearch) {
            lines.add(label.toUpperCase());
            lines.add("");
        }
        for (String cmd : commands) {
            CliHandler handler = dispatcher.handler(cmd);
            T2<String, String> commandLine = handler.commandLine(cmd);
            if (noSearch || commandLine._1.contains(search)) {
                lines.add(S.fmt(fmt, cmd, commandLine._2));
            }
        }
        context.println(S.join("\n", lines));
    }

    private int calMaxCmdLen(List<String> commands) {
        int max = 0;
        for (String c : commands) {
            max = Math.max(max, c.length());
        }
        return max;
    }

    public boolean showHelp(String command, CliContext context) {
        CliHandler handler = context.app().cliDispatcher().handler(command);
        if (null == handler) {
            // context.println("Unrecongized command: %s", command);
            return false;
        }
        List<String> lines = C.newList();

        T2<String, String> commandLine = handler.commandLine(command);
        lines.add("Usage: " + commandLine._1);
        lines.add(commandLine._2);

        String summary = handler.summary();
        if (S.notBlank(summary)) {
            lines.add("");
            lines.add(summary);
        }

        List<T2<String, String>> options = handler.options();
        if (!options.isEmpty()) {
            lines.add("");
            lines.add("Options:");
            int maxLen = 0;
            for (T2<String, String> t2: options) {
                maxLen = Math.max(maxLen, t2._1.length());
            }
            String fmt = "  %-" + (maxLen + 4) + "s %s";
            for (T2<String, String> t2: options) {
                lines.add(S.fmt(fmt, t2._1, t2._2));
            }
        }

        context.println(S.join("\n", lines));
        return true;
    }

    @Override
    public T2<String, String> commandLine(String commandName) {
        return T2("help [options] [command]", "show help information");
    }

    @Override
    public String summary() {
        return "display information about a command if COMMAND is specified. " +
                "Otherwise the list of help topics is printed. \n Note when " +
                "COMMAND is specified, OPTIONS are ignored";
    }

    @Override
    public List<T2<String, String>> options() {
        List<T2<String, String>> retList = C.newList();
        retList.add(T2("-s --system", "list system commands"));
        retList.add(T2("-a --app", "list application commands"));
        return retList;
    }

    private Object readResolve() {
        return INSTANCE;
    }

    public static void updateMaxWidth(int width) {
        maxWidth = Math.max(maxWidth, width);
    }

    public static void main(String[] args) {
        System.out.println(S.fmt("%-18s %s", "acc.create", "create an account"));
    }

}
