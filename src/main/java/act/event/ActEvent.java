package act.event;

import org.osgl.$;

import java.util.EventObject;

public class ActEvent<T> extends EventObject {

    private final long ts;

    public ActEvent(T source) {
        super(source);
        ts = $.ms();
    }

    public final T source() {
        return $.cast(getSource());
    }

    public final long timestamp() {
        return ts;
    }
}
