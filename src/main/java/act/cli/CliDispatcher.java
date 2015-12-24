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
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.List;
import java.util.Map;

/**
 * Dispatch console command to CLI command handler
 */
public class CliDispatcher extends AppServiceBase<CliDispatcher> {

    private static Logger logger = LogManager.get(CliDispatcher.class);

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
        logger.info("Command registered: %s", command);
        return this;
    }

    public boolean registered(String command) {
        return registry.containsKey(command);
    }

    public CliHandler handler(String command) {
        CliHandler handler = registry.get(command);
        if (null == handler && !command.startsWith("act.")) {
            handler = registry.get("act." + command);
        }
        return handler;
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
