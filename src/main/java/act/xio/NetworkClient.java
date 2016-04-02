package act.xio;

import act.Act;
import act.app.ActionContext;
import act.app.App;
import act.app.RequestRefreshClassLoader;
import act.app.RequestServerRestart;
import act.app.util.NamedPort;
import act.handler.RequestHandler;
import act.handler.builtin.controller.RequestHandlerProxy;
import act.handler.event.BeforeCommit;
import act.metric.Metric;
import act.metric.MetricInfo;
import act.metric.Timer;
import act.route.Router;
import act.view.ActServerError;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.mvc.result.Result;
import org.osgl.util.E;
import org.osgl.util.S;

public class NetworkClient extends $.F1<ActionContext, Void> {

    private static Logger logger = LogManager.get(NetworkClient.class);

    final private App app;
    private NamedPort port;
    private Metric metric;

    public NetworkClient(App app) {
        E.NPE(app);
        this.app = app;
        this.metric = Act.metricPlugin().metric("act.http");
    }

    public NetworkClient(App app, NamedPort port) {
        this(app);
        this.port = port;
        this.metric = Act.metricPlugin().metric("act.http");
    }

    public App app() {
        return app;
    }

    public void handle(ActionContext ctx) {
        H.Request req = ctx.req();
        String url = req.url();
        H.Method method = req.method();
        Timer timer = null;
        try {
            if (Act.isDev()) {
                synchronized (app) {
                    try {
                        app.detectChanges();
                    } catch (RequestRefreshClassLoader refreshRequest) {
                        app.refresh();
                    } catch (RequestServerRestart requestServerRestart) {
                        app.refresh();
                    }
                }
            }
            if (app.config().contentSuffixAware()) {
                if (url.endsWith("/json") || url.endsWith(".json")) {
                    url = url.substring(0, url.length() - 5);
                    req.accept(H.Format.JSON);
                } else if (url.endsWith("/xml") || url.endsWith(".xml")) {
                    url = url.substring(0, url.length() - 4);
                    req.accept(H.Format.XML);
                } else if (url.endsWith("/csv") || url.endsWith(".csv")) {
                    url = url.substring(0, url.length() - 4);
                    req.accept(H.Format.CSV);
                }
            }
            timer = metric.startTimer(MetricInfo.ROUTING);
            RequestHandler rh = router().getInvoker(method, url, ctx);
            timer.stop();
            ctx.handler(rh);
            timer = metric.startTimer(S.builder(MetricInfo.HTTP_HANDLER).append(":").append(rh).toString());
            rh.handle(ctx);
        } catch (Result r) {
            try {
                r = RequestHandlerProxy.GLOBAL_AFTER_INTERCEPTOR.apply(r, ctx);
            } catch (Exception e) {
                logger.error(e, "Error calling global after interceptor");
                r = ActServerError.of(e);
            }
            app.eventBus().emit(new BeforeCommit(r, ctx));
            r.apply(req, ctx.resp());
        } catch (Exception t) {
            logger.error(t, "Error handling network request");
            Result r;
            try {
                r = RequestHandlerProxy.GLOBAL_EXCEPTION_INTERCEPTOR.apply(t, ctx);
            } catch (Exception e) {
                logger.error(e, "Error calling global exception interceptor");
                r = ActServerError.of(e);
            }
            if (null == r) {
                r = ActServerError.of(t);
            }
            app.eventBus().emit(new BeforeCommit(r, ctx));
            r.apply(req, ctx.resp());
        } finally {
            // we don't destroy ctx here in case it's been passed to
            // another thread
            ActionContext.clearCurrent();
            if (null != timer) {
                timer.stop();
            }
        }
    }

    @Override
    public Void apply(ActionContext ctx) throws NotAppliedException, $.Break {
        handle(ctx);
        return null;
    }

    @Override
    public String toString() {
        return app().name();
    }

    private Router router() {
        return app.router(port);
    }
}
