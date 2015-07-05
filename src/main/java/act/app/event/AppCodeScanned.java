package act.app.event;

import act.app.App;

/**
 * Emitted immediately when {@link App}'s app code scanner finished its work
 */
public class AppCodeScanned extends AppEvent {
    public AppCodeScanned(App source) {
        super(AppEventId.APP_CODE_SCANNED, source);
    }
}