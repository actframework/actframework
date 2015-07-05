package act.app.event;

import act.app.App;

/**
 * Emitted right before initializing {@link act.app.AppClassLoader}
 */
public class AppPreLoadClasses extends AppEvent {
    public AppPreLoadClasses(App source) {
        super(AppEventId.PRE_LOAD_CLASSES, source);
    }
}
