package act.app.event;

import act.app.App;

/**
 * Mark a Application Post Start event. The App Post Start event
 * will be triggered after the {@link AppStart} event
 */
public class AppPostStart extends AppEvent {
    public AppPostStart(App source) {
        super(AppEventId.POST_START, source);
    }
}
