package act.xio;

import act.app.ActionContext;
import act.app.App;
import act.app.RequestRefreshClassLoader;
import act.app.RequestServerRestart;
import act.app.util.NamedPort;
import act.handler.RequestHandler;
import act.route.Router;
import act.view.ActServerError;
import org.osgl._;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.mvc.result.Result;
import org.osgl.util.E;

public class NetworkClient extends _.F1<ActionContext, Void> {
    private App app;
    private NamedPort port;

    public NetworkClient(App app) {
        E.NPE(app);
        this.app = app;
    }

    public NetworkClient(App app, NamedPort port) {
        this(app);
        this.port = port;
    }

    public App app() {
        return app;
    }

    public void handle(ActionContext ctx) {
        H.Request req = ctx.req();
        String url = req.url();
        H.Method method = req.method();
        try {
            try {
                app.detectChanges();
            } catch (RequestRefreshClassLoader refreshRequest) {
                app.refresh();
            } catch (RequestServerRestart requestServerRestart) {
                app.refresh();
            }
            RequestHandler rh = router().getInvoker(method, url, ctx);
            ctx.handler(rh);
            rh.handle(ctx);
        } catch (Result r) {
            r.apply(req, ctx.resp());
        } catch (Throwable t) {
            Result r = new ActServerError(t, app());
            r.apply(req, ctx.resp());
        } finally {
            // we don't destroy ctx here in case it's been passed to
            // another thread
            ActionContext.clearCurrent();
        }
    }

    @Override
    public Void apply(ActionContext ctx) throws NotAppliedException, _.Break {
        handle(ctx);
        return null;
    }

    private Router router() {
        return app.router(port);
    }
}
