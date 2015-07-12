package act.boot.spark;

import act.app.App;
import act.app.AppContext;
import act.app.ProjectLayout;
import act.boot.ProjectLayoutBuilder;
import act.conf.AppConfig;
import act.controller.Controller;
import act.handler.DelegateRequestHandler;
import act.handler.RequestHandler;
import act.handler.RequestHandlerBase;
import act.handler.builtin.Echo;
import act.handler.builtin.Redirect;
import act.handler.builtin.StaticFileGetter;
import act.route.Router;
import act.view.ActServerError;
import act.xio.NetworkClient;
import act.xio.NetworkService;
import act.xio.undertow.UndertowService;
import org.osgl.http.H;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.mvc.result.Forbidden;
import org.osgl.mvc.result.Result;
import org.osgl.util.C;
import org.osgl.util.E;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static act.boot.spark.SparkApp.Filter.filter;

/**
 * Support Spark framework style app
 */
public final class SparkApp extends App {

    private static Logger logger = L.get(SparkApp.class);

    private static final String GLOBAL = "__G__";

    private static volatile SparkApp app;
    private static volatile boolean started;
    private static NetworkService service;
    private static NetworkClient client;
    private static Map<String, List<RequestHandler>> beforeHandlers = C.newMap();
    private static Map<String, List<RequestHandler>> afterHandlers = C.newMap();
    private static List<Filter> patternMatchedBeforeHandlers = C.newList();
    private static List<Filter> patternMatchedAfterHandlers = C.newList();
    private static C.List<Class<? extends Exception>> registeredExceptions = C.newList();
    private static Map<Class<? extends Exception>, List<RequestHandler>> exceptionHandlers = C.newMap();

    private AppConfig config;
    private Router router;
    private ProjectLayoutBuilder layout;

    private SparkApp() {
        config = new AppConfig();
        router = new Router(this);
        layout = new ProjectLayoutBuilder();
    }

    @Override
    public ProjectLayout layout() {
        return layout;
    }

    @Override
    public AppConfig config() {
        return config;
    }

    @Override
    public Router router() {
        return router;
    }

    private static SparkApp _app() {
        if (null != app) {
            return app;
        }
        synchronized (SparkApp.class) {
            if (null == app) {
                app = new SparkApp();
            }
        }
        return app;
    }

    private static void _start() {
        if (started) return;
        synchronized (SparkApp.class) {
            if (started) return;
            started = true;
            _startAct();
        }
    }

    private static void _startAct() {
        service = new UndertowService();
        client = new NetworkClient(_app());
        service.register(_app().config().port(), client);
        service.start();
    }

    private static Router _router() {
        return _app().router;
    }

    public static void staticFileLocation(String location) {
        staticFileLocation(location, location);
    }

    public static void staticFileLocation(String path, String location) {
        _router().addMapping(H.Method.GET, path,  filteredHandler(path, new StaticFileGetter(location, app)));
        _start();
    }

    public static void externalFileLocation(String location) {
        externalFileLocation(location, location);
    }

    public static void externalFileLocation(String path, String location) {
        _router().addMapping(H.Method.GET, path, filteredHandler(path, new StaticFileGetter(new File(location))));
        _start();
    }

    public static RequestHandler echo(String msg) {
        return new Echo(msg);
    }

    public static RequestHandler redirect(String url) {
        return new Redirect(url);
    }

    public static RequestHandler forbidden() {
        return constant(new Forbidden());
    }

    public static void get(String path, RequestHandler handler) {
        _router().addMapping(H.Method.GET, path, filteredHandler(path, handler));
        _start();
    }

    public static void post(String path, RequestHandler handler) {
        _router().addMapping(H.Method.POST, path, filteredHandler(path, handler));
        _start();
    }

    public static void put(String path, RequestHandler handler) {
        _router().addMapping(H.Method.PUT, path, filteredHandler(path, handler));
        _start();
    }

    public static void delete(String path, RequestHandler handler) {
        _router().addMapping(H.Method.DELETE, path, filteredHandler(path, handler));
        _start();
    }

    public static void on(Class<? extends Exception> e, RequestHandler handler) {
        E.illegalArgumentIf(Result.class.isAssignableFrom(e), "Result is handled by framework already...");
        List<RequestHandler> l = exceptionHandlers.get(e);
        if (null == l) {
            l = C.newList(handler);
            exceptionHandlers.put(e, l);
            registeredExceptions = registeredExceptions.append(e).sort();
        } else {
            l.add(handler);
        }
    }

    public static void before(RequestHandler handler) {
        before(GLOBAL, handler);
    }

    public static void before(String path, RequestHandler handler) {
        RequestHandler handler1 = filter(path, handler);
        if (handler == handler1) {
            addFilter(path, handler, beforeHandlers);
        } else {
            addFilter((Filter) handler1, patternMatchedBeforeHandlers);
        }
    }

    public static void after(RequestHandler handler) {
        after(GLOBAL, handler);
    }

    public static void after(String path, RequestHandler handler) {
        RequestHandler handler1 = filter(path, handler);
        if (handler == handler1) {
            addFilter(path, handler, afterHandlers);
        } else {
            addFilter((Filter) handler1, patternMatchedAfterHandlers);
        }
    }

    private static void addFilter(String path, RequestHandler filter, Map<String, List<RequestHandler>> registry) {
        List<RequestHandler> handlers = registry.get(path);
        if (null == handlers) {
            handlers = C.newList();
            registry.put(path, handlers);
        }
        if (!handlers.contains(filter)) {
            handlers.add(filter);
        }
    }

    private static void addFilter(Filter filter, List<Filter> registry) {
        registry.add(filter);
    }

    private static RequestHandler filteredHandler(String path, RequestHandler handler) {
        return new FilteredHandler(path, (RequestHandlerBase)handler);
    }

    private static RequestHandler constant(final Result result) {
        return new RequestHandlerBase() {
            @Override
            public void handle(AppContext context) {
                throw result;
            }
        };
    }

    static class Filter extends DelegateRequestHandler {
        private Pattern ptn;

        Filter(Pattern ptn, RequestHandler realHandler) {
            super((RequestHandlerBase)realHandler);
            E.NPE(ptn, realHandler);
            this.ptn = ptn;
        }

        boolean matches(String path) {
            return ptn.matcher(path).matches();
        }

        static RequestHandler filter(String path, RequestHandler realHandler) {
            if (path.contains("*") || path.contains("[") || path.contains("(")) {
                return new Filter(Pattern.compile(path), realHandler);
            } else {
                return new FilteredHandler(path, realHandler);
            }
        }
    }

    public abstract static class Handler extends RequestHandlerBase {
        @Override
        public void handle(AppContext context) {
            Result result;
            try {
                Object obj = handle(context.req(), context.resp());
                result = Controller.Util.inferResult(obj, context);
            } catch (Result r) {
                result = r;
            } catch (RuntimeException e) {
                logger.error(e, "Error handling request: %s", e.getMessage());
                result = new ActServerError(e, app);
            }
        }

        public Object handle(H.Request req, H.Response resp) {
            return Controller.Util.ok();
        }
    }

    private static class FilteredHandler extends DelegateRequestHandler {
        private String path;

        FilteredHandler(String path, RequestHandler realHandler) {
            super((RequestHandlerBase)realHandler);
            this.path = path;
        }

        @Override
        public void handle(AppContext ctx) {
            try {
                ctx.saveLocal();
                try {
                    handleBefore(ctx);
                    super.handle(ctx);
                    handleAfter(ctx);
                } catch (Exception e) {
                    onException(e, ctx);
                }
            } catch (Result result) {
                onResult(result, ctx);
            } finally {
                ctx.clearLocal();
            }
        }

        private void onResult(Result result, AppContext context) {
            H.Request req = context.req();
            H.Response resp = context.resp();
            result.apply(req, resp);
        }

        private void onException(Exception e, AppContext ctx) {
            for (Class<? extends Exception> c: registeredExceptions) {
                if (c.isInstance(e)) {
                    List<RequestHandler> l = exceptionHandlers.get(c);
                    for (RequestHandler r: l) {
                        r.handle(ctx);
                    }
                }
            }
        }

        private void handleBefore(AppContext ctx) {
            runFilters(ctx, beforeHandlers);
            runPatternMatchedFilters(ctx, patternMatchedBeforeHandlers);
        }

        private void handleAfter(AppContext ctx) {
            runFilters(ctx, afterHandlers);
            runPatternMatchedFilters(ctx, patternMatchedAfterHandlers);
        }

        private void runFilters(AppContext ctx, Map<String, List<RequestHandler>> staticRegistry) {
            List<RequestHandler> l = staticRegistry.get(GLOBAL);
            if (null != l) {
                runFilters(ctx, l);
            }
            l = staticRegistry.get(path);
            if (null != l) {
                runFilters(ctx, l);
            }
        }

        private void runFilters(AppContext ctx, List<RequestHandler> handlers) {
            for (RequestHandler h : handlers) {
                h.handle(ctx);
            }
        }

        private void runPatternMatchedFilters(AppContext ctx, List<Filter> patternMatchedRegistry) {
            for (Filter f : patternMatchedRegistry) {
                if (f.matches(ctx.req().fullPath())) {
                    f.handle(ctx);
                }
            }
        }
    }


}
