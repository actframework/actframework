package act.app;

import act.cli.ascii_table.ASCIITableHeader;
import act.cli.ascii_table.impl.SimpleASCIITableImpl;
import act.cli.ascii_table.spec.IASCIITable;
import act.cli.ascii_table.spec.IASCIITableAware;
import act.cli.util.CommandLineParser;
import act.util.ActContext;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.cache.CacheService;
import org.osgl.concurrent.ContextLocal;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CliContext extends ActContext.ActContextBase<CliContext> implements IASCIITable {

    private static final ContextLocal<CliContext> _local = $.contextLocal();

    private String commandPath; // e.g. myapp.cli.ListUser

    private Map<String, Object> commanderInstances = C.newMap();

    private PrintWriter pw;

    private CommandLineParser parser;

    private IASCIITable asciiTable;

    private Osgl.T2<? extends Osgl.Function<String, Serializable>, ? extends Osgl.Func2<String, Serializable, ?>> evaluatorCache;

    public CliContext(String line, App app, PrintWriter pw) {
        super(app);
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
        this.pw = $.NPE(pw);
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

    @Override
    public CliContext accept(H.Format fmt) {
        throw E.unsupport();
    }

    @Override
    public H.Format accept() {
        throw E.unsupport();
    }

    public void print(String template, Object... args) {
        pw.printf(template, args);
    }

    public void println(String template, Object... args) {
        print(template, args);
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
            asciiTable = new SimpleASCIITableImpl(pw);
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
