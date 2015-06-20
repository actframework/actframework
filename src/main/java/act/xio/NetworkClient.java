package act.xio;

import org.osgl._;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.mvc.result.NotFound;
import act.app.App;
import act.app.AppContext;
import act.app.RequestRefreshClassLoader;
import act.app.RequestServerRestart;
import act.handler.RequestHandler;
import act.route.Router;
import org.osgl.mvc.result.Result;
import org.osgl.util.E;

public class NetworkClient extends _.F1<AppContext, Void> {
    private App app;

    public NetworkClient(App app) {
        E.NPE(app);
        this.app = app;
    }

    public App app() {
        return app;
    }

    public void handle(AppContext ctx) {
        H.Request req = ctx.req();
        String url = req.url();
        H.Method method = req.method();
        try {
            app.detectChanges();
        } catch (RequestRefreshClassLoader refreshRequest) {
            app.refresh();
        } catch (RequestServerRestart requestServerRestart) {
            app.refresh();
        }
        try {
            RequestHandler rh = router().getInvoker(method, url, ctx);
            rh.handle(ctx);
        } catch (Result r) {
            r.apply(req, ctx.resp());
        } finally {
            // we don't destroy ctx here in case it's been passed to
            // another thread
            AppContext.clearCurrent();
        }
    }

    @Override
    public Void apply(AppContext ctx) throws NotAppliedException, _.Break {
        handle(ctx);
        return null;
    }

    private Router router() {
        return app.router();
    }
}
