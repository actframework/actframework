package act.ws;

import act.app.ActionContext;
import act.event.ActEvent;
import act.xio.WebSocketConnection;

public class WebSocketConnectEvent extends ActEvent<WebSocketConnection> {

    private ActionContext context;

    public WebSocketConnectEvent(WebSocketConnection connection, ActionContext context) {
        super(connection);
    }

    public ActionContext context() {
        return context;
    }
}
