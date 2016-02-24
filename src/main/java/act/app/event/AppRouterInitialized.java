package act.app.event;

import act.app.App;

/**
 * Emitted after Application's router has been initialized and before route table loaded
 */
public class AppRouterInitialized extends AppEvent {
    public AppRouterInitialized(App source) {
        super(AppEventId.ROUTER_INITIALIZED, source);
    }
}
