package act.app;

import act.cli.ascii_table.ASCIITableHeader;
import act.cli.ascii_table.impl.SimpleASCIITableImpl;
import act.cli.ascii_table.spec.IASCIITable;
import act.cli.ascii_table.spec.IASCIITableAware;
import act.cli.util.CommandLineParser;
import act.util.ActContext;
import jline.Terminal2;
import jline.console.ConsoleReader;
import org.osgl.$;
import org.osgl.cache.CacheService;
import org.osgl.concurrent.ContextLocal;
import org.osgl.http.H;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CliContext extends ActContext.ActContextBase<CliContext> implements IASCIITable {

    public static final String ATTR_PWD = "__act_pwd__";

    private static final ContextLocal<CliContext> _local = $.contextLocal();

    private CliSession session;

    private String commandPath; // e.g. myapp.cli.ListUser

    private Map<String, Object> commanderInstances = C.newMap();

    private ConsoleReader console;

    // workaround of issue http://stackoverflow.com/questions/34467383/jline2-print-j-when-it-should-print-n-on-a-telnet-console
    private PrintWriter pw;

    private CommandLineParser parser;

    private IASCIITable asciiTable;

    private CacheService evaluatorCache;

    private boolean rawPrint;


    public CliContext(String line, App app, ConsoleReader console, CliSession session) {
        super(app);
        this.session = session;
        parser = new CommandLineParser(line);
        evaluatorCache = app.cache();
        this.console = $.NPE(console);
        Terminal2 t2 = $.cast(console.getTerminal());
        t2.setEchoEnabled(false);
        this.pw = new PrintWriter(console.getOutput());
        this.rawPrint = null == System.getenv("cli-no-raw-print");
        saveLocal();
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
        prompt("act[" + session.id() + "]>");
    }

    public CacheService evaluatorCache() {
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
        session.setAttribute(key, val);
    }

    /**
     * Fetch the value {@link #attribute(String, Object) attributed} with
     * the key specified in this context
     * @param key the key
     * @param <T> the generic type of the value
     * @return the value object associated with the key
     */
    public <T> T attribute(String key) {
        return session.getAttribute(key);
    }

    /**
     * Return the current working directory
     * @return the current working directory
     */
    public File curDir() {
        File file = attribute(ATTR_PWD);;
        if (null == file) {
            file = new File(System.getProperty("user.dir"));
            attribute(ATTR_PWD, file);
        }
        return file;
    }

    public CliContext chDir(File dir) {
        E.illegalArgumentIf(!dir.isDirectory());
        attribute(ATTR_PWD, dir);
        return this;
    }

    @Override
    public CliContext accept(H.Format fmt) {
        throw E.unsupport();
    }

    @Override
    public H.Format accept() {
        throw E.unsupport();
    }

    private void print0(String template, Object... args) {
        try {
            console.print(S.fmt(template, args));
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    public void flush() {
        pw.flush();
    }

    public void print(String template, Object ... args) {
        if (rawPrint) {
            print1(template, args);
        } else {
            print0(template, args);
        }
    }

    private void print1(String template, Object ... args) {
        if (args.length == 0) {
            pw.print(template);
        } else {
            pw.printf(osNative(template), args);
        }
    }

    private void println0(String template, Object... args) {
        try {
            if (args.length > 0) {
                template = S.fmt(template);
            }
            console.println(template);
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    public void println(String template, Object... args) {
        if (rawPrint) {
            println1(template, args);
        } else {
            println0(template, args);
        }
    }

    private void println1(String template, Object... args) {
        if (args.length == 0) {
            pw.print(template);
        } else {
            pw.printf(osNative(template), args);
        }
        pw.println();
    }

    @Override
    protected void releaseResources() {
        super.releaseResources();
        _local.remove();
    }

    public String commandPath() {
        return commandPath;
    }

    public CliContext commandPath(String path) {
        commandPath = path;
        return this;
    }

    public <T> T newInstance(String className) {
        return app().newInstance(className, this);
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

    public void saveLocal() {
        _local.set(this);
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

    private static String osNative(String s) {
        s = s.replace("\n\r", "\n");
        s = s.replace("\r", "\n");
        if ("\n".equals($.OS.lineSeparator())) {
            return s;
        }
        s = s.replace("\n", $.OS.lineSeparator());
        return s;
    }

}
