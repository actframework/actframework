package act.app.event;

import act.app.App;

/**
 * Mark a Application Stop event
 */
public class AppStop extends AppEvent {
    public AppStop(App source) {
        super(AppEventId.STOP, source);
    }
}
