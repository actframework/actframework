package act.xio;

/**
 * Encapsulate operations provided by underline network service, e.g. netty/undertow etc
 */
public interface NetworkService {
    void register(int port, NetworkClient client);

    void start();

    void shutdown();
}
