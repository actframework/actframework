package act.xio.undertow;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.ActResponse;
import act.app.ActionContext;
import act.app.App;
import act.conf.AppConfig;
import act.sys.SystemAvailabilityMonitor;
import act.xio.NetworkHandler;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.osgl.http.H;
import org.osgl.util.E;

import java.nio.ByteBuffer;

/**
 * Dispatch undertow request to Act application
 */
public class ActHttpHandler implements HttpHandler {

    private final NetworkHandler client;

    private static final ByteBuffer SERVICE_UNAVAILABLE = ByteBuffer.wrap("503 Service Unavailable".getBytes());

    public ActHttpHandler(NetworkHandler client) {
        E.NPE(client);
        this.client = client;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) {
        if (!SystemAvailabilityMonitor.isAvailable()) {
            exchange.setStatusCode(503);
            exchange.getResponseSender().send(SERVICE_UNAVAILABLE.duplicate());
            return;
        }
        ActionContext ctx = createActionContext(exchange);
        client.handle(ctx, new UndertowNetworkDispatcher(exchange));
    }

    private ActionContext createActionContext(HttpServerExchange exchange) {
        App app = client.app();
        AppConfig config = app.config();
        return ActionContext.create(app, req(exchange, config), resp(exchange, config));
    }

    private H.Request req(HttpServerExchange exchange, AppConfig config) {
        return new UndertowRequest(exchange, config);
    }

    private ActResponse<?> resp(HttpServerExchange exchange, AppConfig config) {
        return new UndertowResponse(exchange, config);
    }

}
