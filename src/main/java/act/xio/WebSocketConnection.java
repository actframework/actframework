package act.xio;

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

import act.Destroyable;
import act.conf.AppConfig;

/**
 * A WebSocket connection
 */
public interface WebSocketConnection extends Destroyable {

    /**
     * Session ID of this connection
     * @return connection session id
     */
    String sessionId();

    /**
     * Returns the username which is gained when connection is setup
     * by calling {@link org.osgl.http.H.Session#get(String)} with
     * {@link AppConfig#sessionKeyUsername()}
     *
     * @return the username or `null` if there is no logged in user when connection is setup
     */
    String username();

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
