package act.cli.builtin;

import act.app.CliContext;
import act.cli.util.CliCursor;
import act.handler.CliHandlerBase;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.List;

public class IterateCursor extends CliHandlerBase {

    public static final IterateCursor INSTANCE = new IterateCursor();

    private IterateCursor() {}

    @Override
    public void handle(CliContext context) {
        CliCursor cursor = context.session().cursor();
        if (null == cursor) {
            context.println("no cursor");
        } else {
            cursor.output(context);
        }
    }

    private Object readResolve() {
        return INSTANCE;
    }

    @Override
    public $.T2<String, String> commandLine(String commandName) {
        return $.T2(commandName, "iterate through cursor");
    }

    @Override
    public List<$.T2<String, String>> options() {
        return C.list();
    }

}
