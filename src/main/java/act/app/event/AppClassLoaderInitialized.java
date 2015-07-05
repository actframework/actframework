package act.app.event;

import act.app.App;

/**
 * Emitted immediately when {@link App}'s class loader instance initialized
 */
public class AppClassLoaderInitialized extends AppEvent {
    public AppClassLoaderInitialized(App source) {
        super(AppEventId.CLASS_LOADER_INITIALIZED, source);
    }
}