package act.app;

/**
 * A daemon encapsulate a long running logic that can be
 *
 * * {@link #start() started}
 * * {@link #stop() stopped}
 * * {@link #restart() restart}
 * * Check {@link #state() state}
 */
public interface Daemon extends Runnable {

    public static enum State {
        STOPPING, STOPPED, STARTING, STARTED, ERROR
    }

    /**
     * Start this daemon
     */
    void start();

    /**
     * Stop this daemon
     */
    void stop();

    /**
     * {@link #stop() stop} this daemon first and then {@link #start() start}
     */
    void restart();

    /**
     * Returns the {@link State state} of this daemon
     * @return the state
     */
    State state();

    /**
     * Returns ID of the daemon logic
     * @return ID
     */
    String id();

    /**
     * Returns last error
     * @return the last error encountered
     */
    Exception lastError();

}
