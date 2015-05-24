package act;

import org.osgl.exception.UnexpectedException;

import java.util.concurrent.atomic.AtomicLong;

public class ActException extends UnexpectedException {
    private static AtomicLong atomicLong = new AtomicLong(System.currentTimeMillis());
    private String id;

    public ActException() {
        super();
        setId();
    }

    public ActException(String message) {
        super(message);
        setId();
    }

    public ActException(String message, Object... args) {
        super(message, args);
        setId();
    }

    public ActException(Throwable cause) {
        super(cause);
        setId();
    }

    public ActException(Throwable cause, String message, Object... args) {
        super(cause, message, args);
        setId();
    }

    private void setId() {
        long nid = atomicLong.incrementAndGet();
        id = Long.toString(nid, 26);
    }

    public String id() {
        return id;
    }
}
