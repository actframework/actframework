package act.app.event;

import act.app.App;

/**
 * Emitted after Application's {@link act.event.EventBus} has been initialized
 */
public class EventBusInitialized extends AppEvent {
    public EventBusInitialized(App source) {
        super(AppEventId.EVENT_BUS_INITIALIZED, source);
    }
}
