package act.cli.builtin;

import act.app.CliContext;
import act.cli.CliDispatcher;
import act.handler.CliHandler;
import act.handler.CliHandlerBase;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;
import java.util.Set;

public class Help extends CliHandlerBase {

    public static final Help INSTANCE = new Help();

    private Help() {}

    @Override
    public void handle(CliContext context) {
        CliDispatcher dispatcher = context.app().cliDispatcher();
        List<String> args = context.arguments();
        if (args.size() > 0) {
            String command = args.get(0);
            CliHandler handler = dispatcher.handler(command);
            if (null == handler) {
                context.println("Unrecognized command: %s", command);
            } else {
                context.println(handler.help(command));
            }
        } else {
            List<String> commands = dispatcher.commands();
            StringBuilder sb = S.builder();
            Set<CliHandler> set = C.newSet();
            for (String command : commands) {
                CliHandler handler = dispatcher.handler(command);
                // ensure alias handler not displayed multiple times
                if (set.contains(handler)) {
                    continue;
                } else {
                    set.add(handler);
                }
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(handler.help(command));
            }
            context.println(sb.toString());
        }
    }

    @Override
    public String help(String commandName) {
        return commandName + "\tShow help message";
    }

    private Object readResolve() {
        return INSTANCE;
    }

}
