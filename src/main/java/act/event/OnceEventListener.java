package act.event;

import java.util.EventListener;
import java.util.EventObject;

/**
 * `OnceEventListener` handle event and return a
 * `boolean` value indicate if the event is handled
 * or not
 */
public interface OnceEventListener<EVENT_TYPE extends EventObject> extends EventListener {
    boolean tryHandle(EVENT_TYPE event) throws Exception;
}
