package act.event;

import org.osgl.$;

import java.util.EventObject;

/**
 * An {@code ActEvent} is a generic version of {@link EventObject}
 * @param <T> the generic type (the sub class type) of the event source
 */
public class ActEvent<T> extends EventObject {

    private final long ts;

    public ActEvent(T source) {
        super(source);
        ts = $.ms();
    }

    /**
     * Unlike the {@link Object#getClass()} method, which always return
     * the java class of the current instance. This {@code eventType()}
     * method allow a certain implementation of the class terminate the
     * return value of the method. For example, suppose you have a event
     * class {@code MyEvent} and you might have some anonymous class
     * of {@code MyEvent}. If you implement the {@code eventType()} of
     * {@code MyEvent} class as follows:
     * <pre>
     *     public class&lt;MyEvent&gt; eventType() {
     *         return MyEvent.class;
     *     }
     * </pre>
     * Then all the anonymous sub class will return the {@code MyEvent.class}
     * instead of their own class.
     * <p>This allows the ActFramework properly handle the event class registration</p>
     * @return the type of the event
     */
    public Class<T> eventType() {
        return $.cast(getClass());
    }

    public final T source() {
        return $.cast(getSource());
    }

    /**
     * Return the timestamp of the event
     * @return the timestamp
     * @see System#currentTimeMillis()
     */
    public final long timestamp() {
        return ts;
    }
}
