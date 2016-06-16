package act.xio;

import act.Destroyable;
import act.util.DestroyableBase;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;

import java.io.IOException;
import java.util.Map;

/**
 * The base implementation of {@link NetworkService}
 */
public abstract class NetworkServiceBase  extends DestroyableBase implements NetworkService {

    protected final static Logger logger = LogManager.get(NetworkService.class);

    private volatile boolean started;
    private Map<Integer, NetworkClient> registry = C.newMap();
    private Map<Integer, NetworkClient> failed = C.newMap();

    public synchronized void register(int port, NetworkClient client) {
        E.NPE(client);
        E.illegalArgumentIf(registry.containsKey(port), "Port %s has been registered already", port);
        registry.put(port, client);
        if (started) {
            if (!trySetUpClient(client, port)) {
                failed.put(port, client);
            }
        }
    }

    @Override
    public void start() {
        bootUp();
        for (int port : registry.keySet()) {
            NetworkClient client = registry.get(port);
            if (!trySetUpClient(client, port)) {
                failed.put(port, client);
            }
        }
        started = true;
    }

    @Override
    public void shutdown() {
        close();
    }

    private boolean trySetUpClient(NetworkClient client, int port) {
        try {
            setUpClient(client, port);
            return true;
        } catch (IOException e) {
            logger.warn(e, "Cannot set up %s to port %s:", client, port);
            return false;
        }
    }

    protected abstract void setUpClient(NetworkClient client, int port) throws IOException;

    protected abstract void bootUp();

    protected abstract void close();

    @Override
    protected void releaseResources() {
        super.releaseResources();
        Destroyable.Util.destroyAll(registry.values());
        registry.clear();
    }
}
