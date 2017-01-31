package act.xio;

/**
 * An `NetworkDispatcher` can dispatch a network computation context to a worker thread
 */
public interface NetworkDispatcher {

    void dispatch(NetworkJob job);

}
