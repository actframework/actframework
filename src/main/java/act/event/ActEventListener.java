package act.event;

import act.Destroyable;

import java.util.EventListener;
import java.util.EventObject;

public interface ActEventListener<EVENT_TYPE extends EventObject> extends EventListener, Destroyable {
    void on(EVENT_TYPE event) throws Exception;
}
