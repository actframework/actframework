package act.ws;

import act.util.LogSupportedDestroyableBase;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;

/**
 * The implementation of this interface will be registered automatically
 * and called when a websocket connection is established.
 *
 * Note if the implementation class is annotated with {@link WsEndpoint}
 * then it will match the URL path specified in `WsEndpoint` with the
 * current websocket connection URL. In case there is no URL setting in
 * the `WsEndpoint` then it will receive call on websocket connect event
 * of any URL path
 */
public interface WsConnectionHandler {

    /**
     * Implement this method to process websocket connection.
     *
     * @param context the web socket context
     */
    void handleConnection(WebSocketContext context);

    @Singleton
    class Manager extends LogSupportedDestroyableBase {
        private List<WsConnectionHandler> freeHandlers = new ArrayList<>();
        private Map<String>
    }
}
