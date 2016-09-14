package act.cli.builtin;

import act.cli.CliContext;
import act.handler.CliHandlerBase;
import org.osgl.$;
import org.osgl.util.C;

import java.util.List;

public class Exit extends CliHandlerBase {

    public static final Exit INSTANCE = new Exit();
    public static final $.Break BREAK = new $.Break(true);

    private Exit() {}

    @Override
    public void handle(CliContext context) {
        context.println("bye");
        context.flush();
        throw BREAK;
    }

    private Object readResolve() {
        return INSTANCE;
    }

    @Override
    public $.T2<String, String> commandLine() {
        return $.T2("exit", "exit the console");
    }

    @Override
    public List<$.T2<String, String>> options() {
        return C.list();
    }

}
