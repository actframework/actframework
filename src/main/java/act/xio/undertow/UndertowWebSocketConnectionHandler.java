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

import act.Act;
import act.app.ActionContext;
import act.controller.meta.ActionMethodMetaInfo;
import act.view.ActErrorResult;
import act.ws.WebSocketCloseEvent;
import act.ws.WebSocketConnectEvent;
import act.ws.WebSocketConnectionManager;
import act.ws.WebSocketContext;
import act.xio.WebSocketConnection;
import act.xio.WebSocketConnectionHandler;
import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import java.io.IOException;

class UndertowWebSocketConnectionHandler extends WebSocketConnectionHandler {

    UndertowWebSocketConnectionHandler (WebSocketConnectionManager manager) {
        super(manager);
    }

    UndertowWebSocketConnectionHandler(ActionMethodMetaInfo method, WebSocketConnectionManager manager) {
        super(method, manager);
    }

    @Override
    public void handle(final ActionContext context) {
        final UndertowRequest req = (UndertowRequest) context.req();
        HttpServerExchange exchange = req.exchange();
        try {
            Handlers.websocket(new WebSocketConnectionCallback() {
                @Override
                public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
                    final WebSocketConnection connection = new UndertowWebSocketConnection(channel, context.session());
                    channel.setAttribute("act_conn", connection);
                    connectionManager.registerNewConnection(connection, context);
                    final WebSocketContext wsCtx = new WebSocketContext(req.url(), connection, connectionManager, connectionManager.app());
                    channel.getReceiveSetter().set(new AbstractReceiveListener() {
                        @Override
                        protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
                            String payload = message.getData();
                            wsCtx.messageReceived(payload);
                            invoke(wsCtx);
                        }

                        @Override
                        protected void onClose(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel) throws IOException {
                            super.onClose(webSocketChannel, channel);
                            connection.destroy();
                            context.app().eventBus().trigger(new WebSocketCloseEvent(wsCtx));
                        }
                    });
                    channel.resumeReceives();
                    Act.eventBus().trigger(new WebSocketConnectEvent(wsCtx));
                }

            }).handleRequest(exchange);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw ActErrorResult.of(e);
        }
    }
}
