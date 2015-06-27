package act.app.event;

import org.osgl.util.S;

public abstract class AppEventHandler implements EventChannelListener {
    private String id;
    public AppEventHandler(CharSequence id) {
        if (null == id) {
            id = S.uuid();
        }
        this.id = id.toString();
    }

    public AppEventHandler() {
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
        if (obj instanceof EventChannelListener) {
            EventChannelListener that = (EventChannelListener) obj;
            return S.eq(that.id(), this.id());
        }
        return false;
    }
}
