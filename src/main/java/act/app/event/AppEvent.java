package act.app.event;

import act.app.App;
import act.event.ActEvent;
import act.event.SystemEvent;

public abstract class AppEvent extends ActEvent<App> implements SystemEvent {
    private int id;
    public AppEvent(AppEventId id, App source) {
        super(source);
        this.id = id.ordinal();
    }
    public int id() {
        return id;
    }
}
