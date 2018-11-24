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

import act.Act;
import act.Destroyable;
import act.app.App;
import act.controller.meta.ActionMethodMetaInfo;
import act.util.LogSupportedDestroyableBase;
import act.ws.WebSocketConnectionListener;
import act.ws.WebSocketConnectionManager;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.E;

import java.io.IOException;
import java.util.*;
import javax.enterprise.context.ApplicationScoped;

/**
 * The base implementation of {@link Network}
 */
public abstract class NetworkBase extends LogSupportedDestroyableBase implements Network {

    protected final static Logger logger = LogManager.get(Network.class);

    private volatile boolean started;
    private Map<Integer, NetworkHandler> registry = new HashMap<>();
    private Map<Integer, NetworkHandler> failed = new HashMap<>();
    private Set<Integer> securePorts = new HashSet<>();
    private volatile WebSocketConnectionHandler simpleWebSocketConnector;

    public synchronized void register(int port, boolean secure, NetworkHandler client) {
        E.NPE(client);
        E.illegalArgumentIf(registry.containsKey(port), "Port %s has been registered already", port);
        registry.put(port, client);
        if (secure) {
            securePorts.add(port);
        }
        if (started) {
            if (!trySetUpClient(client, port, secure)) {
                failed.put(port, client);
            } else {
                logger.info("network client hooked on port: %s", port);
            }
        }
    }

    @Override
    public void start() {
        for (int port : registry.keySet()) {
            NetworkHandler client = registry.get(port);
            if (!trySetUpClient(client, port, securePorts.contains(port))) {
                failed.put(port, client);
            } else {
                Act.LOGGER.info("network client hooked on port: %s", port);
            }
        }
        started = true;
        App app = Act.app();
        if (null != app) {
            app.registerHotReloadListener(new App.HotReloadListener() {
                @Override
                public void preHotReload() {
                    simpleWebSocketConnector = null;
                }
            });
        }
    }

    @Override
    public void shutdown() {
        close();
    }

    protected void resetWebSocketConnectionHandler() {
        simpleWebSocketConnector = null;
    }

    private boolean trySetUpClient(NetworkHandler client, int port, boolean secure) {
        try {
            setUpClient(client, port, secure);
            return true;
        } catch (IOException e) {
            logger.warn(e, "Cannot set up %s to port %s:", client, port);
            return false;
        }
    }

    protected abstract void setUpClient(NetworkHandler client, int port, boolean secure) throws IOException;

    public abstract void bootUp();

    protected abstract void close();

    @Override
    protected void releaseResources() {
        super.releaseResources();
        Destroyable.Util.destroyAll(registry.values(), ApplicationScoped.class);
        registry.clear();
    }

    @Override
    public WebSocketConnectionHandler createWebSocketConnectionHandler(ActionMethodMetaInfo methodInfo) {
        WebSocketConnectionManager manager = Act.app().service(WebSocketConnectionManager.class);
        return internalCreateWsConnHandler(methodInfo, manager);
    }

    @Override
    public WebSocketConnectionHandler createWebSocketConnectionHandler() {
        if (null == simpleWebSocketConnector) {
            synchronized (this) {
                if (null == simpleWebSocketConnector) {
                    WebSocketConnectionManager manager = Act.app().service(WebSocketConnectionManager.class);
                    simpleWebSocketConnector = internalCreateWsConnHandler(null, manager);
                }
            }
        }
        return simpleWebSocketConnector;
    }

    @Override
    public WebSocketConnectionHandler createWebSocketConnectionHandler(final WebSocketConnectionListener listener) {
        WebSocketConnectionManager manager = Act.app().service(WebSocketConnectionManager.class);
        WebSocketConnectionHandler handler = internalCreateWsConnHandler(null, manager);
        handler.setConnectionListener(listener);
        return handler;
    }

    protected abstract WebSocketConnectionHandler internalCreateWsConnHandler(ActionMethodMetaInfo methodInfo, WebSocketConnectionManager manager);
}
