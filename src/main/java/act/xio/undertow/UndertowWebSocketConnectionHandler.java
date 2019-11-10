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

import act.app.ActionContext;
import act.controller.meta.ActionMethodMetaInfo;
import act.view.ActErrorResult;
import act.ws.*;
import act.xio.WebSocketConnection;
import act.xio.WebSocketConnectionHandler;
import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import java.io.IOException;

class UndertowWebSocketConnectionHandler extends WebSocketConnectionHandler {

    UndertowWebSocketConnectionHandler(ActionMethodMetaInfo method, WebSocketConnectionManager manager) {
        super(method, manager);
    }

    @Override
    public void handle(final ActionContext context) {
        if (logger.isTraceEnabled()) {
            logger.trace("handle websocket connection request to %s", context.req().url());
        }
        final UndertowRequest req = (UndertowRequest) context.req();
        HttpServerExchange exchange = req.exchange();
        try {
            Handlers.websocket(new WebSocketConnectionCallback() {
                @Override
                public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
                    final WebSocketConnection connection = new UndertowWebSocketConnection(channel, context.session());
                    channel.setAttribute("act_conn", connection);
                    connectionManager.registerNewConnection(connection, context);
                    final WebSocketContext wsCtx = new WebSocketContext(req.url(), connection, connectionManager, context, connectionManager.app());
                    if (logger.isTraceEnabled()) {
                        logger.trace("websocket context[%s] created for %s", connection.sessionId(), context.req().url());
                    }
                    channel.getReceiveSetter().set(new AbstractReceiveListener() {
                        @Override
                        protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
                            WebSocketContext.current(wsCtx);
                            String payload = message.getData();
                            if (logger.isTraceEnabled()) {
                                logger.trace("websocket message received: %s", payload);
                            }
                            wsCtx.messageReceived(payload);
                            invoke(wsCtx);
                        }

                        @Override
                        protected void onClose(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel) throws IOException {
                            if (logger.isTraceEnabled()) {
                                logger.trace("websocket closed: ", connection.sessionId());
                            }
                            WebSocketContext.current(wsCtx);
                            super.onClose(webSocketChannel, channel);
                            connection.destroy();
                            UndertowWebSocketConnectionHandler.this._onClose(wsCtx);
                        }
                    });
                    channel.resumeReceives();
                    UndertowWebSocketConnectionHandler.this._onConnect(wsCtx);
                }

            }).handleRequest(exchange);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw ActErrorResult.of(e);
        }
    }
}
