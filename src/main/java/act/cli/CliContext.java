package act.cli;

import act.app.App;
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
import org.osgl.util.Str;

import java.io.Serializable;
import java.util.*;

public class CliContext extends ActContext.ActContextBase<CliContext> {

    private static final ContextLocal<CliContext> _local = $.contextLocal();

    private String commandPath; // e.g. myapp.cli.ListUser

    private List<String> arguments;

    private Osgl.T2<? extends Osgl.Function<String, Serializable>, ? extends Osgl.Func2<String, Serializable, ?>> evaluatorCache;

    public CliContext(App app) {
        super(app);
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
    }

    public Osgl.T2<? extends Osgl.Function<String, Serializable>, ? extends Osgl.Func2<String, Serializable, ?>> evaluatorCache() {
        return evaluatorCache;
    }

    public CliContext arguments(String ... arguments) {
        this.arguments = C.listOf(arguments);
        return this;
    }

    public List<String> arguments() {
        return arguments;
    }

    @Override
    public CliContext accept(H.Format fmt) {
        throw E.unsupport();
    }

    @Override
    public H.Format accept() {
        throw E.unsupport();
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

    public static CliContext current() {
        return _local.get();
    }

}
