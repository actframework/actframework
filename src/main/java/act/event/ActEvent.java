package act.event;

import org.osgl._;

import java.util.EventObject;

public class ActEvent<T> extends EventObject {

    private final long ts;

    public ActEvent(T source) {
        super(source);
        ts = _.ms();
    }

    public final T source() {
        return _.cast(getSource());
    }

    public final long timestamp() {
        return ts;
    }
}
