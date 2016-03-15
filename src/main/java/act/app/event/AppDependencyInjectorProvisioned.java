package act.app.event;

import act.app.App;

/**
 * Emitted immediately when {@link App}'s dependency injector is provisioned
 */
public class AppDependencyInjectorProvisioned extends AppEvent {
    public AppDependencyInjectorProvisioned(App source) {
        super(AppEventId.DEPENDENCY_INJECTOR_PROVISIONED, source);
    }
}