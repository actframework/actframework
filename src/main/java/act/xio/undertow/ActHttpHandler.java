package act.xio.undertow;

import act.Act;
import act.app.ActionContext;
import act.app.App;
import act.conf.AppConfig;
import act.metric.Metric;
import act.metric.MetricInfo;
import act.metric.Timer;
import act.xio.NetworkHandler;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.osgl.http.H;
import org.osgl.util.E;

/**
 * Dispatch undertow request to Act application
 */
public class ActHttpHandler implements HttpHandler {

    private final NetworkHandler client;
    private Metric metric;

    public ActHttpHandler(NetworkHandler client) {
        E.NPE(client);
        this.client = client;
        this.metric = Act.metricPlugin().metric("act.http");
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
        } else {
            Timer timer = metric.startTimer(MetricInfo.CREATE_CONTEXT);
            ActionContext ctx = createActionContext(exchange);
            timer.stop();
            client.handle(ctx);
        }
    }

    private ActionContext createActionContext(HttpServerExchange exchange) {
        App app = client.app();
        AppConfig config = app.config();
        ActionContext ctx = ActionContext.create(app, req(exchange, config), resp(exchange, config));
        exchange.putAttachment(ActBlockingExchange.KEY_APP_CTX, ctx);
        exchange.startBlocking(new ActBlockingExchange(exchange));
        return ctx;
    }

    private H.Request req(HttpServerExchange exchange, AppConfig config) {
        return new UndertowRequest(exchange, config);
    }

    private H.Response resp(HttpServerExchange exchange, AppConfig config) {
        return new UndertowResponse(exchange, config);
    }
}
