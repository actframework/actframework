package act.cli;

import act.app.ActionContext;
import jline.console.ConsoleReader;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The Cli over http context
 */
public class CliOverHttpContext extends CliContext {

    public CliOverHttpContext(ActionContext actionContext, OutputStream os) {
        super(line(actionContext), actionContext.app(), console(actionContext, os), session(actionContext), false);
    }

    private static String line(ActionContext actionContext) {
        String cmd = null;
        StringBuilder sb = S.builder();
        for (String s : actionContext.paramKeys()) {
            if ("cmd".equals(s)) {
                cmd = actionContext.paramVal(s);
            } else if (s.startsWith("-")) {
                String val = actionContext.paramVal(s);
                if (S.notBlank(val)) {
                    val = val.replaceAll("[\n\r]+", "<br/>");
                    if (val.contains(" ")) {
                        if (val.contains("\"")) {
                            val = S.builder("'").append(val).append("'").toString();
                        } else {
                            val = S.builder("\"").append(val).append("\"").toString();
                        }
                    }
                    if (s.contains(",")) {
                        s = S.before(s, ",");
                    }
                    sb.append(s).append(" ").append(val).append(" ");
                }
            }
        }
        E.illegalArgumentIf(null == cmd, "cmd param required");
        return S.builder(cmd).append(" ").append(sb.toString()).toString();
    }

    private static ConsoleReader console(ActionContext actionContext, OutputStream os) {
        try {
            return new CliOverHttpConsole(actionContext, os);
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    private static CliSession session(ActionContext actionContext) {
        return new CliOverHttpSession(actionContext);
    }
}
