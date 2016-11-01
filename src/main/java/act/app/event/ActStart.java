package act.app.event;

import act.app.App;

/**
 * ActStart event is triggered after Application loaded and network layer started.
 *
 * **Note** this event will NOT be triggered after app reload in dev mode
 */
public class ActStart extends AppEvent {
    public ActStart(App source) {
        super(AppEventId.ACT_START, source);
    }
}
