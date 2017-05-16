package act.xio.undertow;

import act.app.ActionContext;
import act.controller.meta.ActionMethodMetaInfo;
import act.view.ActErrorResult;
import act.ws.WebSocketConnectionManager;
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

    UndertowWebSocketConnectionHandler(ActionMethodMetaInfo method, WebSocketConnectionManager manager) {
        super(method, manager);
    }

    @Override
    public void handle(final ActionContext context) {
        UndertowRequest req = (UndertowRequest) context.req();
        HttpServerExchange exchange = req.exchange();
        try {
            Handlers.websocket(new WebSocketConnectionCallback() {
                @Override
                public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
                    final WebSocketConnection connection = new UndertowWebSocketConnection(channel);
                    connectionManager.registerNewConnection(connection, context);
                    channel.getReceiveSetter().set(new AbstractReceiveListener() {
                        @Override
                        protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
                            String payload = message.getData();
                            invoke(payload, connection);
                        }

                        @Override
                        protected void onClose(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel) throws IOException {
                            super.onClose(webSocketChannel, channel);
                            connection.destroy();
                        }
                    });
                }
            }).handleRequest(exchange);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw ActErrorResult.of(e);
        }
    }
}
