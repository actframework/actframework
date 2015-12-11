package act.cli;

import act.app.App;
import act.app.AppServiceBase;
import act.handler.CliHandler;
import act.handler.builtin.cli.CliHandlerProxy;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.Map;

/**
 * Dispatch console command to CLI command handler
 */
public class CliDispatcher extends AppServiceBase<CliDispatcher> {

    private Map<String, CliHandler> registry = C.newMap();

    public CliDispatcher(App app) {
        super(app);
    }

    public CliDispatcher registerCommandHandler(String command, String handler) {
        if (registry.containsKey(command)) {
            throw E.invalidConfiguration("Command %s already registered");
        }
        registry.put(command, new CliHandlerProxy(handler, app()));
        return this;
    }

    public boolean registered(String command) {
        return registry.containsKey(command);
    }

    public CliHandler handler(String command) {
        return registry.get(command);
    }

    @Override
    protected void releaseResources() {
        registry.clear();
    }
}
