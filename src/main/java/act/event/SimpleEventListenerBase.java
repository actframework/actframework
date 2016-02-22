package act.event;

import act.app.event.AppEventListener;
import act.util.DestroyableBase;
import org.osgl.util.S;

import java.util.EventObject;

public abstract class SimpleEventListenerBase extends DestroyableBase implements SimpleEventListener {
    private String id;
    public SimpleEventListenerBase(CharSequence id) {
        if (null == id) {
            id = S.uuid();
        }
        this.id = id.toString();
    }

    public SimpleEventListenerBase() {
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
        if (obj instanceof SimpleEventListener) {
            SimpleEventListener that = (SimpleEventListener) obj;
            return S.eq(that.id(), this.id());
        }
        return false;
    }
}
