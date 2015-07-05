package act.event;

import act.Destroyable;

import java.util.EventListener;

public interface ActEventListener<EVENT_TYPE extends ActEvent> extends EventListener, Destroyable {
    void on(EVENT_TYPE event);
}
