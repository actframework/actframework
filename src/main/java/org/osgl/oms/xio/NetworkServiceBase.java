package org.osgl.oms.xio;

import org.osgl.util.C;
import org.osgl.util.E;

import java.util.Map;

/**
 * The base implementation of {@link NetworkService}
 */
public abstract class NetworkServiceBase implements NetworkService {

    private volatile boolean started;
    private Map<Integer, NetworkClient> registry = C.newMap();

    public synchronized void register(int port, NetworkClient client) {
        E.NPE(client);
        E.illegalArgumentIf(registry.containsKey(port), "Port %s has been registered already", port);
        registry.put(port, client);
        if (started) {
            clientRegistered(client, port);
        }
    }

    @Override
    public void start() {
        bootUp();
        for (int port : registry.keySet()) {
            NetworkClient client = registry.get(port);
            clientRegistered(client, port);
        }
    }

    @Override
    public void shutdown() {
        close();
        registry.clear();
    }

    protected abstract void clientRegistered(NetworkClient client, int port);

    protected abstract void bootUp();

    protected abstract void close();
}
