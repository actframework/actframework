package org.osgl.oms;

import org.osgl.exception.UnexpectedException;

import java.util.concurrent.atomic.AtomicLong;

public class OmsException extends UnexpectedException {
    private static AtomicLong atomicLong = new AtomicLong(System.currentTimeMillis());
    private String id;

    public OmsException() {
        super();
        setId();
    }

    public OmsException(String message) {
        super(message);
        setId();
    }

    public OmsException(String message, Object... args) {
        super(message, args);
        setId();
    }

    public OmsException(Throwable cause) {
        super(cause);
        setId();
    }

    public OmsException(Throwable cause, String message, Object... args) {
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
