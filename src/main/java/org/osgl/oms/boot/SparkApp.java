package org.osgl.oms.boot;

import org.osgl.http.H;
import org.osgl.mvc.result.NotFound;
import org.osgl.mvc.result.Ok;
import org.osgl.mvc.result.Result;
import org.osgl.oms.app.App;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.app.ProjectLayout;
import org.osgl.oms.conf.AppConfig;
import org.osgl.oms.handler.RequestHandler;
import org.osgl.oms.handler.RequestHandlerBase;
import org.osgl.oms.handler.builtin.Echo;
import org.osgl.oms.handler.builtin.Redirect;
import org.osgl.oms.handler.builtin.StaticFileGetter;
import org.osgl.oms.route.Router;
import org.osgl.oms.xio.NetworkClient;
import org.osgl.oms.xio.NetworkService;
import org.osgl.oms.xio.undertow.UndertowService;

/**
 * Support Spark framework style app
 */
public class SparkApp extends App {

    private static volatile SparkApp app;
    private static volatile boolean started;
    private static NetworkService service;
    private static NetworkClient client;
    private static String staticFileLocation;

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
            _startOms();
        }
    }

    private static void _startOms() {
        service = new UndertowService();
        client = new NetworkClient(_app());
        service.register(_app().config().port(), client);
        service.start();
    }


    private static Router _router() {
        return _app().router;
    }

    public static void staticFileLocation(String location) {
        _router().addMapping(H.Method.GET, location, new StaticFileGetter(location, false));
    }

    public static RequestHandler echo(String msg) {
        return new Echo(msg);
    }

    public static RequestHandler redirect(String url) {
        return new Redirect(url);
    }

    public static void get(String path, RequestHandler handler) {
        _router().addMapping(H.Method.GET, path, handler);
        _start();
    }

    public static void post(String path, RequestHandler handler) {
        _router().addMapping(H.Method.POST, path, handler);
        _start();
    }

    public static void put(String path, RequestHandler handler) {
        _router().addMapping(H.Method.PUT, path, handler);
        _start();
    }

    public static void delete(String path, RequestHandler handler) {
        _router().addMapping(H.Method.DELETE, path, handler);
        _start();
    }

    public abstract static class Handler extends RequestHandlerBase {
        @Override
        public void handle(AppContext context) {
            try {
                context.saveLocal();
                handle(context.req(), context.resp());
            } catch (Result r) {
                onResult(r, context);
            } finally {
                AppContext.clear();
            }
        }

        public abstract void handle(H.Request req, H.Response resp);

        private static void onResult(Result result, AppContext context) {
            H.Request req = context.req();
            H.Response resp = context.resp();
            result.apply(req, resp);
        }
    }
}
