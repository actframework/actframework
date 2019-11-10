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

import act.util.LogSupportedDestroyableBase;
import act.xio.WebSocketConnection;
import org.osgl.$;
import org.osgl.util.C;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Organize websocket connection by string typed keys. Multiple connections
 * can be attached to the same key
 */
public class WebSocketConnectionRegistry extends LogSupportedDestroyableBase {

    private ConcurrentMap<String, ConcurrentMap<WebSocketConnection, WebSocketConnection>> registry = new ConcurrentHashMap<>();

    private ReentrantLock lock = new ReentrantLock();

    /**
     * Return a list of websocket connection by key
     *
     * @param key
     *         the key to find the websocket connection list
     * @return a list of websocket connection or an empty list if no websocket connection found by key
     */
    public List<WebSocketConnection> get(String key) {
        final List<WebSocketConnection> retList = new ArrayList<>();
        accept(key, C.F.addTo(retList));
        return retList;
    }

    /**
     * Remove all connection associations to `key`.
     *
     * @param key
     *         the key to be removed from the registry
     */
    public void removeAll(String key) {
        registry.remove(key);
    }

    /**
     * Accept a visitor to iterate through the connections attached to the key specified
     *
     * @param key
     *         the key
     * @param visitor
     *         the visitor
     */
    public void accept(String key, $.Function<WebSocketConnection, ?> visitor) {
        ConcurrentMap<WebSocketConnection, WebSocketConnection> connections = registry.get(key);
        if (null == connections) {
            return;
        }
        if (!connections.isEmpty()) {
            lock.lock();
            try {
                List<WebSocketConnection> toBeCleared = null;
                for (WebSocketConnection conn : connections.keySet()) {
                    if (conn.closed()) {
                        if (null == toBeCleared) {
                            toBeCleared = new ArrayList<>();
                        }
                        toBeCleared.add(conn);
                        continue;
                    }
                    visitor.apply(conn);
                }
                if (null != toBeCleared) {
                    ConcurrentMap<WebSocketConnection, WebSocketConnection> originalCopy = registry.get(key);
                    originalCopy.keySet().removeAll(toBeCleared);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Alias of {@link #signIn(String, WebSocketConnection)}
     *
     * Register a connection to the registry by key.
     *
     * Note multiple connections can be attached to the same key
     *
     * @param key
     *         the key
     * @param connection
     *         the websocket connection
     * @see #signIn(String, WebSocketConnection)
     */
    public void register(String key, WebSocketConnection connection) {
        signIn(key, connection);
    }

    /**
     * Sign in a connection to the registry by key.
     *
     * Note multiple connections can be attached to the same key
     *
     * @param key
     *         the key
     * @param connection
     *         the websocket connection
     * @see #register(String, WebSocketConnection)
     */
    public void signIn(String key, WebSocketConnection connection) {
        ConcurrentMap<WebSocketConnection, WebSocketConnection> bag = ensureConnectionList(key);
        bag.put(connection, connection);
    }

    /**
     * Sign in a group of web socket connections to the registry by key
     *
     * @param key
     *         the key
     * @param connections
     *         a collection of websocket connections
     */
    public void register(String key, Collection<WebSocketConnection> connections) {
        signIn(key, connections);
    }

    /**
     * Sign in a group of connections to the registry by key
     *
     * @param key
     *         the key
     * @param connections
     *         a collection of websocket connections
     */
    public void signIn(String key, Collection<WebSocketConnection> connections) {
        if (connections.isEmpty()) {
            return;
        }
        Map<WebSocketConnection, WebSocketConnection> newMap = new HashMap<>();
        for (WebSocketConnection conn : connections) {
            newMap.put(conn, conn);
        }
        ConcurrentMap<WebSocketConnection, WebSocketConnection> bag = ensureConnectionList(key);
        bag.putAll(newMap);
    }

    /**
     * De-register a connection from the registry by key specified
     *
     * @param key
     *         the key
     * @param connection
     *         the websocket connection
     */
    public void deRegister(String key, WebSocketConnection connection) {
        signOff(key, connection);
    }

    /**
     * De-register a group of connections from the registry by key
     *
     * Note this method is an alias of {@link #signOff(String, Collection)}
     *
     * @param key
     *         the key
     * @param connections
     *         a collection of websocket connections
     * @see #signOff(String, Collection)
     */
    public void deRegister(String key, Collection<WebSocketConnection> connections) {
        signOff(key, connections);
    }

    /**
     * Detach a connection from a key.
     *
     * @param key
     *         the key
     * @param connection
     *         the connection
     */
    public void signOff(String key, WebSocketConnection connection) {
        ConcurrentMap<WebSocketConnection, WebSocketConnection> connections = registry.get(key);
        if (null == connections) {
            return;
        }
        connections.remove(connection);
    }

    /**
     * Remove a connection from this registry.
     *
     * This method is an alias of {@link #signOff(WebSocketConnection)}.
     *
     * @param connection
     *         the connection.
     */
    public void deRegister(WebSocketConnection connection) {
        signOff(connection);
    }

    /**
     * Remove a connection from all keys.
     *
     * @param connection
     *         the connection
     */
    public void signOff(WebSocketConnection connection) {
        for (ConcurrentMap<WebSocketConnection, WebSocketConnection> connections : registry.values()) {
            connections.remove(connection);
        }
    }

    /**
     * Sign off a group of connections from the registry by key
     *
     * @param key
     *         the key
     * @param connections
     *         a collection of websocket connections
     */
    public void signOff(String key, Collection<WebSocketConnection> connections) {
        if (connections.isEmpty()) {
            return;
        }
        ConcurrentMap<WebSocketConnection, WebSocketConnection> bag = ensureConnectionList(key);
        bag.keySet().removeAll(connections);
    }


    /**
     * Returns the connection count in this registry.
     *
     * Note it might count connections that are closed but not removed from registry yet
     *
     * @return the connection count
     */
    public int count() {
        int n = 0;
        for (ConcurrentMap<?, ?> bag : registry.values()) {
            n += bag.size();
        }
        return n;
    }

    /**
     * Returns the connection count by key specified in this registry
     *
     * Note it might count connections that are closed but not removed from registry yet
     *
     * @param key
     *         the key
     * @return connection count by key
     */
    public int count(String key) {
        ConcurrentMap<WebSocketConnection, WebSocketConnection> bag = registry.get(key);
        return null == bag ? 0 : bag.size();
    }

    @Override
    protected void releaseResources() {
        for (ConcurrentMap<WebSocketConnection, WebSocketConnection> connections : registry.values()) {
            for (WebSocketConnection conn : connections.keySet()) {
                conn.destroy();
            }
        }
        registry.clear();
    }

    void purge(List<WebSocketConnection> closedConnections) {
        lock.lock();
        try {
            for (WebSocketConnection connection : closedConnections) {
                for (ConcurrentMap<WebSocketConnection, WebSocketConnection> map: registry.values()) {
                    map.remove(connection);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private ConcurrentMap<WebSocketConnection, WebSocketConnection> ensureConnectionList(String key) {
        ConcurrentMap<WebSocketConnection, WebSocketConnection> connections = registry.get(key);
        if (null == connections) {
            ConcurrentMap<WebSocketConnection, WebSocketConnection> newConnections = newConnectionBag();
            connections = registry.putIfAbsent(key, newConnections);
            if (null == connections) {
                connections = newConnections;
            }
        }
        return connections;
    }

    private ConcurrentMap<WebSocketConnection, WebSocketConnection> newConnectionBag() {
        // TODO find a better strategy to keep track of the connections
        // see http://stackoverflow.com/questions/44040637/best-practice-to-track-websocket-connections-in-java/
        return new ConcurrentHashMap<>();
    }
}
