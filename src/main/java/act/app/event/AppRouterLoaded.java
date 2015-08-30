package act.app.event;

import act.app.App;

/**
 * Emitted after Application's router has been loaded
 */
public class AppRouterLoaded extends AppEvent {
    public AppRouterLoaded(App source) {
        super(AppEventId.ROUTER_LOADED, source);
    }
}
