package act.cli.builtin;

import act.app.CliContext;
import act.handler.CliHandlerBase;
import org.osgl.util.E;

public class Exit extends CliHandlerBase {

    public static final Exit INSTANCE = new Exit();

    private Exit() {}

    @Override
    public void handle(CliContext context) {
        throw E.unsupport();
    }

    private Object readResolve() {
        return INSTANCE;
    }

    @Override
    public String help(String commandName) {
        return commandName + "\tExit act console";
    }
}
