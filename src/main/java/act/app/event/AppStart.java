package act.app.event;

import act.app.App;

/**
 * Mark a Application Start event
 */
public class AppStart extends AppEvent {
    public AppStart(App source) {
        super(AppEventId.START, source);
    }
}
