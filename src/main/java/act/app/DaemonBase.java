package act.app;

import act.util.SingletonBase;

/**
 * The base implementation of {@link Daemon}
 */
@SuppressWarnings("unused")
public abstract class DaemonBase extends SingletonBase implements Daemon {

    private State state = State.STOPPED;
    private Exception lastError;

    @Override
    public final void restart() {
        try {
            stop();
        } finally {
            start();
        }
    }

    @Override
    public final void start() {
        synchronized (this) {
            if (state == State.STARTED || state == State.STARTING) {
                return;
            }
            state = State.STARTING;
        }
        try {
            setup();
            App.instance().jobManager().now(this);
        } catch (Exception e) {
            onException(e, "error starting daemon: %s", id());
            return;
        }
        synchronized (this) {
            state = State.STARTED;
        }
    }

    @Override
    public final void stop() {
        synchronized (this) {
            if (state == State.STOPPED || state == State.STOPPING) {
                return;
            }
            state = State.STOPPING;
        }
        try {
            teardown();
        } catch (Exception e) {
            onException(e, "error stopping daemon: %s", id());
            return;
        }
        synchronized (this){
            state = State.STOPPED;
        }
    }

    @Override
    public final void run() {
        try {
            doJob();
        } catch (Exception e) {
            onException(e, "Error executing daemon: %s", id());
        }
    }

    /**
     * Execute the main logic
     */
    protected abstract void doJob() throws Exception;

    @Override
    public synchronized State state() {
        return state;
    }

    protected void setup() {}

    protected void teardown() {}

    @Override
    protected void releaseResources() {
        try {
            stop();
        } finally {
            super.releaseResources();
        }
    }

    /**
     * Returns the full class name of this daemon.
     *
     * Sub class can override this method to provide short version
     *
     * @return id of this daemon
     */
    @Override
    public String id() {
        return getClass().getName();
    }

    @Override
    public Exception lastError() {
        return lastError;
    }

    protected synchronized void onException(Exception e, String message, Object... args) {
        this.lastError = e;
        this.state = State.ERROR;
        logger.error(e, message, args);
    }

    /**
     * Set last error without updating the state and logging the message
     * @param e the error
     */
    protected synchronized void setLastError(Exception e) {
        this.lastError = e;
    }

}
