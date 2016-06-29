package act.xio;

import act.Destroyable;

/**
 * Encapsulate operations provided by underline network service, e.g. netty/undertow etc
 */
public interface Network extends Destroyable {
    void register(int port, NetworkHandler client);

    void start();

    void shutdown();
}
