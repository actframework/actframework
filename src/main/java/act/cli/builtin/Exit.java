package act.cli.builtin;

import act.app.CliContext;
import act.handler.CliHandlerBase;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.List;

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
    public $.T2<String, String> commandLine(String commandName) {
        return $.T2(commandName, "exit the console");
    }

    @Override
    public List<$.T2<String, String>> options() {
        return C.list();
    }

}
