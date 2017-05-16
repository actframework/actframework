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
import act.controller.meta.ActionMethodMetaInfo;
import act.util.DestroyableBase;
import act.ws.WebSocketConnectionManager;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.util.Map;

/**
 * The base implementation of {@link Network}
 */
public abstract class NetworkBase extends DestroyableBase implements Network {

    protected final static Logger logger = LogManager.get(Network.class);

    private volatile boolean started;
    private Map<Integer, NetworkHandler> registry = C.newMap();
    private Map<Integer, NetworkHandler> failed = C.newMap();

    public synchronized void register(int port, NetworkHandler client) {
        E.NPE(client);
        E.illegalArgumentIf(registry.containsKey(port), "Port %s has been registered already", port);
        registry.put(port, client);
        if (started) {
            if (!trySetUpClient(client, port)) {
                failed.put(port, client);
            } else {
                logger.info("network client hooked on port: %s", port);
            }
        }
    }

    @Override
    public void start() {
        bootUp();
        for (int port : registry.keySet()) {
            NetworkHandler client = registry.get(port);
            if (!trySetUpClient(client, port)) {
                failed.put(port, client);
            } else {
                Act.LOGGER.info("network client hooked on port: %s", port);
            }
        }
        started = true;
    }

    @Override
    public void shutdown() {
        close();
    }

    private boolean trySetUpClient(NetworkHandler client, int port) {
        try {
            setUpClient(client, port);
            return true;
        } catch (IOException e) {
            logger.warn(e, "Cannot set up %s to port %s:", client, port);
            return false;
        }
    }

    protected abstract void setUpClient(NetworkHandler client, int port) throws IOException;

    protected abstract void bootUp();

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

    protected abstract WebSocketConnectionHandler internalCreateWsConnHandler(ActionMethodMetaInfo methodInfo, WebSocketConnectionManager manager);
}
