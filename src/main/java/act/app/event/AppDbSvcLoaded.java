package act.app.event;

import act.app.App;

/**
 * Emitted after Application's {@link act.db.DbService db services} have been loaded
 */
public class AppDbSvcLoaded extends AppEvent {
    public AppDbSvcLoaded(App source) {
        super(AppEventId.DB_SVC_LOADED, source);
    }
}
