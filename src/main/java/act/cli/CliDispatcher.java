package act.cli;

import act.app.App;
import act.app.AppServiceBase;
import act.app.CliContext;
import act.cli.builtin.Exit;
import act.cli.builtin.Help;
import act.cli.meta.CommandMethodMetaInfo;
import act.cli.util.CommandLineParser;
import act.handler.CliHandler;
import act.handler.builtin.cli.CliHandlerProxy;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.List;
import java.util.Map;

/**
 * Dispatch console command to CLI command handler
 */
public class CliDispatcher extends AppServiceBase<CliDispatcher> {

    private Map<String, CliHandler> registry = C.newMap();

    public CliDispatcher(App app) {
        super(app);
        registerBuiltInHandlers();
    }

    public CliDispatcher registerCommandHandler(String command, CommandMethodMetaInfo methodMetaInfo) {
        if (registry.containsKey(command)) {
            throw E.invalidConfiguration("Command %s already registered");
        }
        registry.put(command, new CliHandlerProxy(methodMetaInfo, app()));
        return this;
    }

    public boolean registered(String command) {
        return registry.containsKey(command);
    }

    public CliHandler handler(String command) {
        return registry.get(command);
    }

    /**
     * Returns all commands in alphabetic order
     * @return the list of commands
     */
    public List<String> commands() {
        return C.list(registry.keySet()).sorted();
    }

    @Override
    protected void releaseResources() {
        registry.clear();
    }

    private void registerBuiltInHandlers() {
        registry.put("exit", Exit.INSTANCE);
        registry.put("quit", Exit.INSTANCE);
        registry.put("bye", Exit.INSTANCE);
        registry.put("help", Help.INSTANCE);
    }
}
