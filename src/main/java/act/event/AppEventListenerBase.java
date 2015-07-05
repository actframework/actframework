package act.event;

import act.app.event.AppEvent;
import act.app.event.AppEventListener;
import org.osgl.util.S;

public abstract class AppEventListenerBase<EVENT_TYPE extends AppEvent> extends ActEventListenerBase<EVENT_TYPE> implements AppEventListener<EVENT_TYPE> {
    private String id;
    public AppEventListenerBase(CharSequence id) {
        if (null == id) {
            id = S.uuid();
        }
        this.id = id.toString();
    }

    public AppEventListenerBase() {
        this(S.uuid());
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AppEventListener) {
            AppEventListener that = (AppEventListener) obj;
            return S.eq(that.id(), this.id());
        }
        return false;
    }

}
