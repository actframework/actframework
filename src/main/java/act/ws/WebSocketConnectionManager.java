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

import act.app.ActionContext;
import act.app.App;
import act.app.AppServiceBase;
import act.xio.WebSocketConnection;
import com.alibaba.fastjson.JSON;

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
     * @param tag the tag label
     */
    public void sendToTagged(String message, String tag) {
        sendToConnections(message, tagRegistry(), tag);
    }

    /**
     * Send JSON representation of given data object to all connections tagged with
     * given label
     * @param data the data object
     * @param tag the tag lable
     */
    public void sendJsonToTagged(Object data, String tag) {
        sendToTagged(JSON.toJSONString(data), tag);
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
        app().eventBus().trigger(new WebSocketConnectEvent(connection, context));
    }

    @Override
    protected void releaseResources() {
        bySessionId.destroy();
        byUsername.destroy();
        byUrl.destroy();
        byTag.destroy();
    }

    private void sendToConnections(String message, WebSocketConnectionRegistry registry, String key) {
        for (WebSocketConnection conn : registry.get(key)) {
            conn.send(message);
        }
    }
}
