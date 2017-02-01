package act.xio.undertow;

import act.app.ActionContext;
import act.app.App;
import act.conf.AppConfig;
import act.xio.NetworkDispatcher;
import act.xio.NetworkHandler;
import act.xio.NetworkJob;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.osgl.http.H;
import org.osgl.util.E;

/**
 * Dispatch undertow request to Act application
 */
public class ActHttpHandler implements HttpHandler {

    private final NetworkHandler client;

    public ActHttpHandler(NetworkHandler client) {
        E.NPE(client);
        this.client = client;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        ActionContext ctx = createActionContext(exchange);
        client.handle(ctx, new NetworkDispatcher() {
            @Override
            public void dispatch(NetworkJob job) {
                exchange.dispatch(job);
            }
        });
    }

    private ActionContext createActionContext(HttpServerExchange exchange) {
        App app = client.app();
        AppConfig config = app.config();
        return ActionContext.create(app, req(exchange, config), resp(exchange, config));
    }

    private H.Request req(HttpServerExchange exchange, AppConfig config) {
        return new UndertowRequest(exchange, config);
    }

    private H.Response resp(HttpServerExchange exchange, AppConfig config) {
        return new UndertowResponse(exchange, config);
    }

}
