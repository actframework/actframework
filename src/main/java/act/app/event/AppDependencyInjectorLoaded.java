package act.app.event;

import act.app.App;

/**
 * Emitted immediately when {@link App}'s dependency injector is loaded
 */
public class AppDependencyInjectorLoaded extends AppEvent {
    public AppDependencyInjectorLoaded(App source) {
        super(AppEventId.DEPENDENCY_INJECTOR_LOADED, source);
    }
}