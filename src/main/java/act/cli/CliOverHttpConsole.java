package act.cli;

import act.app.ActionContext;
import jline.console.ConsoleReader;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A `ConsoleReader` for cli over http
 */
public class CliOverHttpConsole extends ConsoleReader {

    public CliOverHttpConsole(ActionContext actionContext, OutputStream os) throws IOException {
        super(actionContext.req().inputStream(), os);
    }
}
