package act.app;

import act.util.SingletonBase;
import org.joda.time.DateTime;
import org.osgl.exception.ConfigurationException;
import org.osgl.util.C;

import java.util.Map;

import static act.app.App.LOGGER;

/**
 * The base implementation of {@link Daemon}
 */
@SuppressWarnings("unused")
public abstract class DaemonBase extends SingletonBase implements Daemon {

    private State state = State.STOPPED;
    private Exception lastError;
    private DateTime ts = DateTime.now();
    private DateTime errTs;
    private Map<String, Object> attributes = C.newMap();

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
            setState(State.STARTING);
        }
        try {
            setup();
            App.instance().jobManager().now(this);
        } catch (Exception e) {
            onException(e, "error starting daemon: %s", id());
            return;
        }
        synchronized (this) {
            setState(State.STARTED);
        }
        LOGGER.info("Daemon[%s] started", id());
    }

    @Override
    public final void stop() {
        synchronized (this) {
            if (state == State.STOPPED || state == State.STOPPING) {
                return;
            }
            setState(State.STOPPING);
        }
        try {
            teardown();
        } catch (Exception e) {
            onException(e, "error stopping daemon: %s", id());
            return;
        }
        synchronized (this){
            setState(State.STOPPED);
        }
        LOGGER.info("Daemon[%s] stopped", id());
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

    @Override
    public DateTime timestamp() {
        return ts;
    }

    protected void setup() throws Exception {}

    protected void teardown() throws Exception {}

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

    @Override
    public DateTime errorTimestamp() {
        return errTs;
    }

    @Override
    public synchronized void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public synchronized void removeAttribute(String key) {
        attributes.remove(key);
    }

    @Override
    public synchronized  <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    public Map<String, Object> getAttributes() {
        return C.newMap(attributes);
    }

    protected synchronized void onException(Exception e, String message, Object... args) {
        this.lastError = e;
        setState(errorState(e));
        LOGGER.error(e, message, args);
    }

    private State errorState(Exception e) {
        return isFatal(e) ? State.FATAL : State.ERROR;
    }

    protected boolean isFatal(Exception e) {
        return e instanceof ConfigurationException;
    }

    /**
     * Set last error without updating the state and logging the message
     * @param e the error
     */
    protected synchronized void setLastError(Exception e) {
        this.lastError = e;
    }

    private void setState(State state) {
        this.state = state;
        ts = DateTime.now();
        if (state == State.ERROR) {
            errTs = ts;
        }
    }

}
