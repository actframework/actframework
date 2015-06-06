package act.app.event;

import act.app.App;
import act.app.AppServiceBase;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.List;

public class AppEventManager extends AppServiceBase<AppEventManager> {

    private List<AppEventListener> listeners = C.newList();

    public AppEventManager(App app) {
        super(app);
    }

    @Override
    protected void releaseResources() {
        listeners.clear();
    }

    public AppEventManager register(AppEventListener listener) {
        E.NPE(listener);
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
        return this;
    }

    public void emitEvent(AppEvent event) {
        for (AppEventListener listener : listeners) {
            listener.handleAppEvent(event);
        }
    }
}
