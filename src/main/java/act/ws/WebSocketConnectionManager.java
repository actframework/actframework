package act.ws;

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

import act.app.*;
import act.app.event.SysEventId;
import act.util.Stateless;
import act.xio.WebSocketConnection;
import com.alibaba.fastjson.JSON;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * Manage {@link WebSocketConnection} through {@link WebSocketConnectionRegistry}
 */
@Stateless
public class WebSocketConnectionManager extends AppServiceBase<WebSocketConnectionManager> {

    private static final Logger logger = LogManager.get(WebSocketConnectionManager.class);

    private final WebSocketConnectionRegistry bySessionId = new WebSocketConnectionRegistry();
    private final WebSocketConnectionRegistry byUsername = new WebSocketConnectionRegistry();
    private final WebSocketConnectionRegistry byUrl = new WebSocketConnectionRegistry();
    private final WebSocketConnectionRegistry byTag = new WebSocketConnectionRegistry();

    private final ConcurrentMap<WebSocketConnection, WebSocketConnection> closed = new ConcurrentHashMap<>();

    private String wsTicketKey;

    public WebSocketConnectionManager(final App app) {
        super(app);
        wsTicketKey = app.config().wsTicketKey();
        app.jobManager().every(new Runnable() {
            @Override
            public void run() {
                purgeClosed();
            }
        }, app.config().wsPurgeClosedConnPeriod(), TimeUnit.SECONDS);
        app.jobManager().on(SysEventId.SINGLETON_PROVISIONED, new Runnable() {
            @Override
            public void run() {
                WebSocketConnectionListener.Manager manager = app.getInstance(WebSocketConnectionListener.Manager.class);
                manager.freeListeners.add(new WebSocketConnectionListener() {
                    @Override
                    public void onConnect(WebSocketContext context) {
                    }

                    @Override
                    public void onClose(WebSocketContext context) {
                        closed.put(context, context);
                        closed.put(context.connection(), context.connection());
                    }
                });
            }
        });
    }

    public WebSocketConnectionRegistry sessionRegistry() {
        return bySessionId;
    }

    public WebSocketConnectionRegistry usernameRegistry() {
        return byUsername;
    }

    public WebSocketConnectionRegistry urlRegistry() {
        return byUrl;
    }

    public WebSocketConnectionRegistry tagRegistry() {
        return byTag;
    }

    /**
     * Add tag to any websocket connection linked to the session specified
     * @param session the session used to find websocket connections
     * @param tag the tag to subscribe
     */
    public void subscribe(H.Session session, final String tag) {
        sessionRegistry().accept(session.id(), new $.Visitor<WebSocketConnection>() {
            @Override
            public void visit(WebSocketConnection connection) throws $.Break {
                byTag.register(tag, connection);
            }
        });
    }

    /**
     * Send message to all connections connected to give URL
     * @param message the message
     * @param url the url
     */
    public void sendToUrl(String message, String url) {
        sendToConnections(message, urlRegistry(), url);
    }

    /**
     * Send JSON representation of given data object to all connections
     * connected to given URL
     *
     * @param data the data object
     * @param url the url
     */
    public void sendJsonToUrl(Object data, String url) {
        sendToUrl(JSON.toJSONString(data), url);
    }

    /**
     * Send message to all connections tagged with given label
     * @param message the message
     * @param label the tag label
     */
    public void sendToTagged(String message, String label) {
        sendToConnections(message, tagRegistry(), label);
    }

    /**
     * Send message to all connections tagged with all given tags
     * @param message the message
     * @param labels the tag labels
     */
    public void sendToTagged(String message, String ... labels) {
        for (String label : labels) {
            sendToTagged(message, label);
        }
    }

    /**
     * Send message to all connections tagged with all given tags
     * @param message the message
     * @param labels the tag labels
     */
    public void sendToTagged(String message, Collection<String> labels) {
        for (String label : labels) {
            sendToTagged(message, label);
        }
    }

    /**
     * Send JSON representation of given data object to all connections tagged with
     * given label
     * @param data the data object
     * @param label the tag label
     */
    public void sendJsonToTagged(Object data, String label) {
        sendToTagged(JSON.toJSONString(data), label);
    }

    /**
     * Send JSON representation of given data object to all connections tagged with all give tag labels
     * @param data the data object
     * @param labels the tag labels
     */
    public void sendJsonToTagged(Object data, String ... labels) {
        for (String label : labels) {
            sendJsonToTagged(data, label);
        }
    }

    /**
     * Send JSON representation of given data object to all connections tagged with all give tag labels
     * @param data the data object
     * @param labels the tag labels
     */
    public void sendJsonToTagged(Object data, Collection<String> labels) {
        for (String label : labels) {
            sendJsonToTagged(data, label);
        }
    }

    /**
     * Send message to all connections of a user
     * @param message the message
     * @param username the username
     */
    public void sendToUser(String message, String username) {
        sendToConnections(message, usernameRegistry(), username);
    }

    /**
     * Send JSON representation of given data object to all connections of a user
     * @param data the data object
     * @param username the username
     */
    public void sendJsonToUser(Object data, String username) {
        sendToUser(JSON.toJSONString(data), username);
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
    }

    @Override
    protected void releaseResources() {
        bySessionId.destroy();
        byUsername.destroy();
        byUrl.destroy();
        byTag.destroy();
    }

    private void purgeClosed() {
        if (closed.isEmpty()) {
            return;
        }
        List<WebSocketConnection> list = C.list(closed.values());
        closed.clear();
        purgeClosed(list, bySessionId);
        purgeClosed(list, byTag);
        purgeClosed(list, byUrl);
        purgeClosed(list, byUsername);
    }

    private void purgeClosed(List<WebSocketConnection> closed, WebSocketConnectionRegistry registry) {
        try {
            registry.purge(closed);
        } catch (Exception e) {
            warn(e, "Error purge closed connection");
        }
    }

    private void sendToConnections(String message, WebSocketConnectionRegistry registry, String key) {
        for (WebSocketConnection conn : registry.get(key)) {
            if (logger.isTraceEnabled()) {
                logger.trace("send to websocket connection by key: %s", key);
            }
            conn.send(message);
        }
    }
}
