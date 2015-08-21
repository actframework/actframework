package act.app.event;

import act.event.ActEventListener;

public interface AppEventListener<EVENT_TYPE extends AppEvent> extends ActEventListener<EVENT_TYPE> {
}
