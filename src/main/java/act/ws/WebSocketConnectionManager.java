package act.ws;

import act.app.ActionContext;
import act.app.App;
import act.app.AppServiceBase;
import act.xio.WebSocketConnection;

/**
 * Manage {@link WebSocketConnection} through {@link WebSocketConnectionRegistry}
 */
public class WebSocketConnectionManager extends AppServiceBase<WebSocketConnectionManager> {

    private final WebSocketConnectionRegistry bySessionId = new WebSocketConnectionRegistry();
    private final WebSocketConnectionRegistry byUsername = new WebSocketConnectionRegistry();
    private final WebSocketConnectionRegistry byUrl = new WebSocketConnectionRegistry();
    private final WebSocketConnectionRegistry byTag = new WebSocketConnectionRegistry();

    private String wsTicketKey;

    public WebSocketConnectionManager(App app) {
        super(app);
        wsTicketKey = app.config().wsTicketKey();
    }

    public void registerNewConnection(WebSocketConnection connection, ActionContext context) {
        bySessionId.register(context.session().id(), connection);
        String username = context.username();
        if (null == username) {
            username = context.paramVal(wsTicketKey);
        }
        if (null != username) {
            byUsername.register(username, connection);
        }
        String url = context.req().url();
        byUrl.register(url, connection);
        app().eventBus().trigger(new WebSocketConnectEvent(connection, context));
    }

    @Override
    protected void releaseResources() {
        bySessionId.destroy();
        byUsername.destroy();
        byUrl.destroy();
        byTag.destroy();
    }
}
