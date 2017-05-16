package act.xio;

import act.Destroyable;

/**
 * A WebSocket connection
 */
public interface WebSocketConnection extends Destroyable {

    /**
     * Send a text message through websocket
     * @param message the text message
     */
    void send(String message);

    /**
     * Close the connection. Note if there are any `IOException`
     * raised by the underline network layer, it will be ignored
     */
    void close();

    /**
     * Check if the connection has been closed
     * @return `true` if connection is closed
     */
    boolean closed();

}
