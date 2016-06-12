package act.event;

import java.util.EventObject;

public abstract class OnceEventListenerBase<EVENT_TYPE extends EventObject>
        extends ActEventListenerBase<EVENT_TYPE>
        implements OnceEventListener<EVENT_TYPE> {

    public OnceEventListenerBase(CharSequence id) {
        super(id);
    }

    public OnceEventListenerBase() {
        super();
    }

    @Override
    public final void on(EVENT_TYPE event) throws Exception {
        tryHandle(event);
    }
}
