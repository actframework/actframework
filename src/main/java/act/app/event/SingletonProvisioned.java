package act.app.event;

import act.app.App;

/**
 * Emitted immediately when {@link App}'s all component annotated with {@link javax.inject.Singleton} has been provisioned
 */
public class SingletonProvisioned extends AppEvent {
    public SingletonProvisioned(App source) {
        super(AppEventId.SINGLETON_PROVISIONED, source);
    }
}