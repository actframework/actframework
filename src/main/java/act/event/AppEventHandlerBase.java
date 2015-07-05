package act.event;

import org.osgl.util.S;

@Deprecated
public abstract class AppEventHandlerBase implements AppEventHandler {
    private String id;
    public AppEventHandlerBase(CharSequence id) {
        if (null == id) {
            id = S.uuid();
        }
        this.id = id.toString();
    }

    public AppEventHandlerBase() {
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
        if (obj instanceof AppEventHandler) {
            AppEventHandler that = (AppEventHandler) obj;
            return S.eq(that.id(), this.id());
        }
        return false;
    }
}
