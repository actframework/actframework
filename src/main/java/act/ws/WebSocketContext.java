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
import act.util.ActContext;
import act.xio.WebSocketConnection;
import com.alibaba.fastjson.JSON;
import org.osgl.$;
import org.osgl.concurrent.ContextLocal;
import org.osgl.http.H;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.*;

public class WebSocketContext extends ActContext.Base<WebSocketContext> implements WebSocketConnection {

    private WebSocketConnection connection;
    private WebSocketConnectionManager manager;
    private ActionContext actionContext;
    private String url;
    private String stringMessage;
    private boolean isJson;
    private Map<String, List<String>> queryParams;

    private static final ContextLocal<WebSocketContext> _local = $.contextLocal();

    public WebSocketContext(
            String url,
            WebSocketConnection connection,
            WebSocketConnectionManager manager,
            ActionContext actionContext,
            App app
    ) {
        super(app);
        this.url = url;
        this.connection = $.requireNotNull(connection);
        this.manager = $.requireNotNull(manager);
        this.actionContext = $.requireNotNull(actionContext);
        _local.set(this);
    }

    public String url() {
        return url;
    }

    @Override
    public String sessionId() {
        return connection.sessionId();
    }

    public H.Session session() {
        return actionContext.session();
    }

    @Override
    public String username() {
        return connection.username();
    }

    public WebSocketConnectionManager manager() {
        return manager;
    }

    public WebSocketConnection connection() {
        return connection;
    }

    public ActionContext actionContext() {
        return actionContext;
    }

    /**
     * Called when remote end send a message to this connection
     * @param receivedMessage the message received
     * @return this context
     */
    public WebSocketContext messageReceived(String receivedMessage) {
        this.stringMessage = S.string(receivedMessage).trim();
        isJson = stringMessage.startsWith("{") || stringMessage.startsWith("]");
        tryParseQueryParams();
        return this;
    }

    /**
     * Tag the websocket connection hold by this context with label specified
     * @param label the label used to tag the websocket connection
     * @return this context
     */
    public WebSocketContext tag(String label) {
        manager.tagRegistry().register(label, connection);
        return this;
    }

    /**
     * Re-Tag the websocket connection hold by this context with label specified.
     * This method will remove all previous tags on the websocket connection and then
     * tag it with the new label.
     * @param label the label.
     * @return this websocket conext.
     */
    public WebSocketContext reTag(String label) {
        WebSocketConnectionRegistry registry = manager.tagRegistry();
        registry.signOff(connection);
        registry.signIn(label, connection);
        return this;
    }

    /**
     * Remove the websocket connection hold by this context from label specified
     * @param label the label previously tagged the websocket connection
     * @return this context
     */
    public WebSocketContext removeTag(String label) {
        manager.tagRegistry().deRegister(label, connection);
        return this;
    }

    public String stringMessage() {
        return stringMessage;
    }

    public boolean isJson() {
        return isJson;
    }

    /**
     * Send a message to the connection of this context
     * @param message the message to be sent
     * @return this context
     */
    public WebSocketContext sendToSelf(String message) {
        send(message);
        return this;
    }

    /**
     * Send JSON representation of a data object to the connection of this context
     * @param data the data to be sent
     * @return this context
     */
    public WebSocketContext sendJsonToSelf(Object data) {
        send(JSON.toJSONString(data));
        return this;
    }

    /**
     * Send message to all connections connected to the same URL of this context with
     * the connection of this context excluded
     *
     * @param message the message to be sent
     * @return this context
     */
    public WebSocketContext sendToPeers(String message) {
        return sendToPeers(message, false);
    }

    /**
     * Send message to all connections connected to the same URL of this context
     *
     * @param message the message to be sent
     * @param excludeSelf whether the connection of this context should be sent to
     * @return this context
     */
    public WebSocketContext sendToPeers(String message, boolean excludeSelf) {
        return sendToConnections(message, url, manager.urlRegistry(), excludeSelf);
    }

    /**
     * Send JSON representation of a data object to all connections connected to
     * the same URL of this context with the connection of this context excluded
     *
     * @param data the data to be sent
     * @return this context
     */
    public WebSocketContext sendJsonToPeers(Object data) {
        return sendToPeers(JSON.toJSONString(data));
    }

    /**
     * Send JSON representation of a data object to all connections connected to
     * the same URL of this context with the connection of this context excluded
     *
     * @param data the data to be sent
     * @param excludeSelf whether it should send to the connection of this context
     * @return this context
     */
    public WebSocketContext sendJsonToPeers(Object data, boolean excludeSelf) {
        return sendToPeers(JSON.toJSONString(data), excludeSelf);
    }

    /**
     * Send message to all connections labeled with tag specified
     * with self connection excluded
     *
     * @param message the message to be sent
     * @param tag the string that tag the connections to be sent
     * @return this context
     */
    public WebSocketContext sendToTagged(String message, String tag) {
        return sendToTagged(message, tag, false);
    }

    /**
     * Send message to all connections labeled with tag specified.
     *
     * @param message the message to be sent
     * @param tag the string that tag the connections to be sent
     * @param excludeSelf specify whether the connection of this context should be send
     * @return this context
     */
    public WebSocketContext sendToTagged(String message, String tag, boolean excludeSelf) {
        return sendToConnections(message, tag, manager.tagRegistry(), excludeSelf);
    }

    /**
     * Send JSON representation of a data object to all connections connected to
     * the same URL of this context with the connection of this context excluded
     *
     * @param data the data to be sent
     * @param tag the tag label
     * @return this context
     */
    public WebSocketContext sendJsonToTagged(Object data, String tag) {
        return sendToTagged(JSON.toJSONString(data), tag);
    }

    /**
     * Send JSON representation of a data object to all connections connected to
     * the same URL of this context with the connection of this context excluded
     *
     * @param data the data to be sent
     * @param tag the tag label
     * @param excludeSelf whether it should send to the connection of this context
     * @return this context
     */
    public WebSocketContext sendJsonToTagged(Object data, String tag, boolean excludeSelf) {
        return sendToTagged(JSON.toJSONString(data), tag, excludeSelf);
    }


    /**
     * Send message to all connections of a certain user
     *
     * @param message the message to be sent
     * @param username the username
     * @return this context
     */
    public WebSocketContext sendToUser(String message, String username) {
        return sendToConnections(message, username, manager.usernameRegistry(), true);
    }

    /**
     * Send JSON representation of a data object to all connections of a certain user
     *
     * @param data the data to be sent
     * @param username the username
     * @return this context
     */
    public WebSocketContext sendJsonToUser(Object data, String username) {
        return sendToTagged(JSON.toJSONString(data), username);
    }

    private WebSocketContext sendToConnections(String message, String key, WebSocketConnectionRegistry registry, boolean excludeSelf) {
        for (WebSocketConnection conn : registry.get(key)) {
            if (!excludeSelf || connection != conn) {
                conn.send(message);
            }
        }
        return this;
    }

    @Override
    public void send(String message) {
        connection.send(message);
    }

    @Override
    public void close() {
        connection.close();
    }

    @Override
    public boolean closed() {
        return connection.closed();
    }

    private void tryParseQueryParams() {
        queryParams = new HashMap<>();
        List<String> stringMessageList = new ArrayList<>();
        stringMessageList.add(stringMessage);
        queryParams.put("_body", stringMessageList);
        if (isJson) {
            return;
        }
        String[] sa = stringMessage.split("&");
        for (String si : sa) {
            String[] pair = si.split("=");
            if (pair.length == 2) {
                String key = pair[0];
                String val = pair[1];
                List<String> list = queryParams.get(key);
                if (null == list) {
                    list = new ArrayList<>();
                    queryParams.put(key, list);
                }
                list.add(val);
            }
        }
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
        throw E.unsupport();
    }

    @Override
    public Set<String> paramKeys() {
        return queryParams.keySet();
    }

    @Override
    public String paramVal(String key) {
        List<String> vals = queryParams.get(key);
        return null == vals || vals.isEmpty() ? null : vals.get(0);
    }

    @Override
    public String[] paramVals(String key) {
        List<String> vals = queryParams.get(key);
        return null == vals ? new String[0] : vals.toArray(new String[vals.size()]);
    }

    public static WebSocketContext current() {
        return _local.get();
    }

    public static void current(WebSocketContext ctx) {
        _local.set(ctx);
    }
}
