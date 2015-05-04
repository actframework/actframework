package org.osgl.oms.xio.undertow;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.osgl.http.H;
import org.osgl.oms.app.App;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.conf.AppConfig;
import org.osgl.oms.xio.NetworkClient;
import org.osgl.util.E;

/**
 * Dispatch undertow request to OMS application
 */
public class OmsHttpHandler implements HttpHandler {

    private final NetworkClient client;

    public OmsHttpHandler(NetworkClient client) {
        E.NPE(client);
        this.client = client;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        AppContext ctx = createAppContext(exchange);
        client.handle(ctx);
    }

    private AppContext createAppContext(HttpServerExchange exchange) {
        App app = client.app();
        AppConfig config = app.config();
        AppContext ctx = AppContext.create(app, req(exchange, config), resp(exchange, config));
        exchange.putAttachment(OmsBlockingExchange.KEY_APP_CTX, ctx);
        exchange.startBlocking(new OmsBlockingExchange(exchange));
        return ctx;
    }

    private H.Request req(HttpServerExchange exchange, AppConfig config) {
        return new UndertowRequest(exchange, config);
    }

    private H.Response resp(HttpServerExchange exchange, AppConfig config) {
        return new UndertowResponse(exchange, config);
    }
}
