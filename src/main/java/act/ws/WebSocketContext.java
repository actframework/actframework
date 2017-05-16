package act.ws;

import act.app.App;
import act.util.ActContext;
import act.xio.WebSocketConnection;
import org.osgl.$;
import org.osgl.concurrent.ContextLocal;
import org.osgl.http.H;
import org.osgl.util.E;

import java.util.Set;

public class WebSocketContext extends ActContext.Base<WebSocketContext> {

    private WebSocketConnection connection;
    private WebSocketConnectionManager manager;
    private String url;
    private String methodPath;
    private String stringMessage;

    private static final ContextLocal<WebSocketContext> _local = $.contextLocal();

    public WebSocketContext(
            String methodPath,
            String url,
            WebSocketConnection connection,
            WebSocketConnectionManager manager,
            App app
    ) {
        super(app);
        this.methodPath = methodPath;
        this.url = url;
        this.connection = $.notNull(connection);
        this.manager = $.notNull(manager);
        _local.set(this);
    }

    public WebSocketContext fullTextMessage(String receivedMessage) {
        this.stringMessage = receivedMessage;
        return this;
    }



    @Override
    public WebSocketContext accept(H.Format fmt) {
        throw E.unsupport();
    }

    @Override
    public H.Format accept() {
        throw E.unsupport();
    }

    @Override
    public String methodPath() {
        return methodPath;
    }

    @Override
    public Set<String> paramKeys() {
        throw E.unsupport();
    }

    @Override
    public String paramVal(String key) {
        throw E.unsupport();
    }

    @Override
    public String[] paramVals(String key) {
        throw E.unsupport();
    }

    public static WebSocketContext current() {
        return _local.get();
    }
}
