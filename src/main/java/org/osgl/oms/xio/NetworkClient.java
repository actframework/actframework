package org.osgl.oms.xio;

import org.osgl._;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.mvc.result.Result;
import org.osgl.oms.app.App;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.handler.RequestHandler;
import org.osgl.oms.route.Router;
import org.osgl.util.E;

public class NetworkClient extends _.F1<AppContext, Void>{
    private App app;

    public NetworkClient(App app) {
        E.NPE(app);
        this.app = app;
    }

    public void handle(AppContext ctx) {
        H.Request req = ctx.req();
        String url = req.url();
        H.Method method = req.method();
        try {
            RequestHandler rh = router().getInvoker(method, url, ctx);
            rh.handle(ctx);
        } catch (Result r) {
            r.apply(req, ctx.resp());
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
