package act.event;

import act.Act;
import act.app.event.AppEventListener;
import act.util.DestroyableBase;
import org.osgl.util.S;

import java.util.EventObject;

public abstract class ActEventListenerBase<EVENT_TYPE extends EventObject> extends DestroyableBase implements ActEventListener<EVENT_TYPE> {
    private String id;
    public ActEventListenerBase(CharSequence id) {
        if (null == id) {
            id = Act.cuid();
        }
        this.id = id.toString();
    }

    public ActEventListenerBase() {
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
    public final boolean equals(Object obj) {
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
