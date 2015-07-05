package act.app.event;

import act.app.App;

/**
 * Emitted immediately when {@link App}'s class loader has loaded classes
 */
public class AppClassLoaded extends AppEvent {
    public AppClassLoaded(App source) {
        super(AppEventId.CLASS_LOADED, source);
    }
}