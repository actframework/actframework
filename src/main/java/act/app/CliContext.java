package act.app;

import act.Destroyable;
import act.cli.ascii_table.ASCIITableHeader;
import act.cli.ascii_table.impl.SimpleASCIITableImpl;
import act.cli.ascii_table.spec.IASCIITable;
import act.cli.ascii_table.spec.IASCIITableAware;
import act.cli.util.CommandLineParser;
import act.util.ActContext;
import jline.Terminal2;
import jline.console.ConsoleReader;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.cache.CacheService;
import org.osgl.concurrent.ContextLocal;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.util.C;
import org.osgl.util.Crypto;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CliContext extends ActContext.ActContextBase<CliContext> implements IASCIITable {

    private static final ContextLocal<CliContext> _local = $.contextLocal();

    private String sessionId;

    private String commandPath; // e.g. myapp.cli.ListUser

    private Map<String, Object> commanderInstances = C.newMap();

    private ConsoleReader console;

    // workaround of issue http://stackoverflow.com/questions/34467383/jline2-print-j-when-it-should-print-n-on-a-telnet-console
    private PrintWriter pw;

    private CommandLineParser parser;

    private IASCIITable asciiTable;

    private Osgl.T2<? extends Osgl.Function<String, Serializable>, ? extends Osgl.Func2<String, Serializable, ?>> evaluatorCache;

    /**
     * Allow user command to attach data to the context and fetched for later use.
     * <p>
     *     A typical usage scenario is user command wants to set up a "context" for the
     *     following commands. However it shall provide a command to exit the "context"
     * </p>
     */
    private Map<String, Object> attributes = C.newMap();

    public CliContext(String id, String line, App app, ConsoleReader console) {
        super(app);
        this.sessionId = id;
        parser = new CommandLineParser(line);
        final CacheService cache = app.cache();
        Osgl.F1<String, Serializable> getter = new Osgl.F1<String, Serializable>() {
            @Override
            public Serializable apply(String s) throws NotAppliedException, Osgl.Break {
                return cache.get(s);
            }
        };
        Osgl.F2<String, Serializable, Object> setter = new Osgl.F2<String, Serializable, Object>() {
            @Override
            public Object apply(String s, Serializable serializable) throws NotAppliedException, Osgl.Break {
                cache.put(s, serializable);
                return null;
            }
        };
        evaluatorCache = Osgl.T2(getter, setter);
        this.console = $.NPE(console);
        Terminal2 t2 = $.cast(console.getTerminal());
        t2.setEchoEnabled(false);
        this.pw = new PrintWriter(console.getOutput());
    }

    /**
     * Set the console prompt
     * @param prompt the prompt
     */
    public void prompt(String prompt) {
        console.setPrompt(prompt);
    }

    /**
     * Reset the console prompt to "{@code act[<session-id>]>}"
     */
    public void resetPrompt() {
        prompt("act[" + sessionId + "]>");
    }

    /**
     * Returns the Cli session ID
     * @return the session ID
     */
    public String sessionId() {
        return sessionId;
    }

    public Osgl.T2<? extends Osgl.Function<String, Serializable>, ? extends Osgl.Func2<String, Serializable, ?>> evaluatorCache() {
        return evaluatorCache;
    }

    public CommandLineParser commandLine() {
        return parser;
    }

    public String command() {
        return parser.command();
    }

    public List<String> arguments() {
        return parser.arguments();
    }

    /**
     * Associate a value to a key in this context
     * @param key the key
     * @param val the value
     */
    public void attribute(String key, Object val) {
        attributes.put(key, val);
    }

    /**
     * Fetch the value {@link #attribute(String, Object) attributed} with
     * the key specified in this context
     * @param key the key
     * @param <T> the generic type of the value
     * @return the value object associated with the key
     */
    public <T> T attribute(String key) {
        return $.cast(attributes.get(key));
    }

    @Override
    public CliContext accept(H.Format fmt) {
        throw E.unsupport();
    }

    @Override
    public H.Format accept() {
        throw E.unsupport();
    }

    public void print0(String template, Object... args) {
        try {
            console.print(S.fmt(template, args));
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    public void print(String template, Object ... args) {
        pw.printf(template, args);
    }

    public void println0(String template, Object... args) {
        try {
            console.println(S.fmt(template, args));
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    public void println(String template, Object... args) {
        pw.printf(template, args);
        pw.println();
    }

    @Override
    protected void releaseResources() {
        super.releaseResources();
        _local.remove();
        Destroyable.Util.tryDestroyAll(attributes.values());
        attributes.clear();
    }

    public String commandPath() {
        return commandPath;
    }

    public CliContext commandPath(String path) {
        commandPath = path;
        return this;
    }

    public <T> T newInstance(Class<? extends T> clazz) {
        if (clazz == CliContext.class) return $.cast(this);
        return app().newInstance(clazz, this);
    }

    public CliContext __commanderInstance(String className, Object instance) {
        if (null == commanderInstances) {
            commanderInstances = C.newMap();
        }
        commanderInstances.put(className, instance);
        return this;
    }

    public Object __commanderInstance(String className) {
        return null == commanderInstances ? null : commanderInstances.get(className);
    }

    /**
     * If {@link #templatePath(String) template path has been set before} then return
     * the template path. Otherwise returns the {@link #commandPath()}
     * @return either template path or action path if template path not set before
     */
    public String templatePath() {
        String path = super.templatePath();
        if (S.notBlank(path)) {
            return path;
        } else {
            return commandPath().replace('.', '/');
        }
    }

    @Override
    public CliContext templatePath(String templatePath) {
        return super.templatePath(templatePath);
    }

    @Override
    public <T> T renderArg(String name) {
        return super.renderArg(name);
    }

    @Override
    public CliContext renderArg(String name, Object val) {
        return super.renderArg(name, val);
    }

    @Override
    public Map<String, Object> renderArgs() {
        return super.renderArgs();
    }

    /**
     * Called by bytecode enhancer to set the name list of the render arguments that is update
     * by the enhancer
     * @param names the render argument names separated by ","
     * @return this AppContext
     */
    public CliContext __appRenderArgNames(String names) {
        return renderArg("__arg_names__", C.listOf(names.split(",")));
    }

    public List<String> __appRenderArgNames() {
        return renderArg("__arg_names__");
    }

    @Override
    public Locale locale() {
        return config().locale();
    }

    private synchronized IASCIITable tbl() {
        if (asciiTable == null) {
            asciiTable = new SimpleASCIITableImpl(new PrintWriter(console.getOutput()));
        }
        return asciiTable;
    }

    @Override
    public void printTable(String[] header, String[][] data) {
        tbl().printTable(header, data);
    }

    @Override
    public void printTable(String[] header, String[][] data, int dataAlign) {
        tbl().printTable(header, data, dataAlign);
    }

    @Override
    public void printTable(String[] header, int headerAlign, String[][] data, int dataAlign) {
        tbl().printTable(header, headerAlign, data, dataAlign);
    }

    @Override
    public void printTable(ASCIITableHeader[] headerObjs, String[][] data) {
        tbl().printTable(headerObjs, data);
    }

    @Override
    public void printTable(IASCIITableAware asciiTableAware) {
        tbl().printTable(asciiTableAware);
    }

    @Override
    public String getTable(String[] header, String[][] data) {
        return tbl().getTable(header, data);
    }

    @Override
    public String getTable(String[] header, String[][] data, int dataAlign) {
        return tbl().getTable(header, data, dataAlign);
    }

    @Override
    public String getTable(String[] header, int headerAlign, String[][] data, int dataAlign) {
        return tbl().getTable(header, headerAlign, data, dataAlign);
    }

    @Override
    public String getTable(ASCIITableHeader[] headerObjs, String[][] data) {
        return tbl().getTable(headerObjs, data);
    }

    @Override
    public String getTable(IASCIITableAware asciiTableAware) {
        return tbl().getTable(asciiTableAware);
    }

    public static CliContext current() {
        return _local.get();
    }

}
