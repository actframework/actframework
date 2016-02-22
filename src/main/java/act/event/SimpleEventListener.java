package act.event;

import act.Destroyable;

import java.util.EventListener;
import java.util.EventObject;

/**
 * Unlike {@link ActEventListener} a {@code SimpleEventListener} can listen to any type of object as event.
 */
public interface SimpleEventListener extends EventListener, Destroyable {
    void on(Object event, Object... payload);
    String id();
}
