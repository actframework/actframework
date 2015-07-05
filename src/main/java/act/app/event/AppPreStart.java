package act.app.event;

import act.app.App;

/**
 * Emit right before emitting {@link AppStart} event
 */
public class AppPreStart extends AppEvent {
    public AppPreStart(App source) {
        super(AppEventId.PRE_START, source);
    }
}
