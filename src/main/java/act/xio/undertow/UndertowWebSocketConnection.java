package act.xio.undertow;

import act.util.DestroyableBase;
import act.xio.WebSocketConnection;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.osgl.$;

import java.io.IOException;

public class UndertowWebSocketConnection extends DestroyableBase implements WebSocketConnection {

    private WebSocketChannel channel;

    public UndertowWebSocketConnection(WebSocketChannel channel) {
        this.channel = $.notNull(channel);
    }

    @Override
    public void send(String message) {
        WebSockets.sendText(message, channel, null);
    }

    @Override
    protected void releaseResources() {
        try {
            channel.sendClose();
        } catch (IOException e) {
            // ignore
        }
    }

    @Override
    public void close() {
        destroy();
    }

    @Override
    public boolean closed() {
        return isDestroyed();
    }
}
