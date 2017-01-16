package act.event;

import act.app.event.AppEventListener;
import act.util.DestroyableBase;
import org.osgl.util.S;

import java.util.EventObject;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ActEventListenerBase<EVENT_TYPE extends EventObject> extends DestroyableBase implements ActEventListener<EVENT_TYPE> {

    private static final AtomicInteger ID_ = new AtomicInteger();

    private String id;
    public ActEventListenerBase(CharSequence id) {
        if (null == id) {
            id = genId();
        }
        this.id = id.toString();
    }

    public ActEventListenerBase() {
        this(genId());
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

    private static String genId() {
        return S.random(3) + ID_.getAndIncrement();
    }

}
