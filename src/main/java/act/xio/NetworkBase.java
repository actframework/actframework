package act.xio;

import act.Act;
import act.Destroyable;
import act.util.DestroyableBase;
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
                Act.logger.info("network client hooked on port: %s", port);
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
}
