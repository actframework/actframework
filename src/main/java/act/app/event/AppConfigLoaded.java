package act.app.event;

import act.app.App;

/**
 * Emitted after Application's configuration has been loaded
 */
public class AppConfigLoaded extends AppEvent {
    public AppConfigLoaded(App source) {
        super(AppEventId.CONFIG_LOADED, source);
    }
}
