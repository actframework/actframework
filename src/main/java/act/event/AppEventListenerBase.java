package act.event;

import act.app.event.AppEvent;
import act.app.event.AppEventListener;

public abstract class AppEventListenerBase<EVENT_TYPE extends AppEvent> extends ActEventListenerBase<EVENT_TYPE> implements AppEventListener<EVENT_TYPE> {
    public AppEventListenerBase(CharSequence id) {
        super(id);
    }

    public AppEventListenerBase() {
        super();
    }
}
