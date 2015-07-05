package act.event;

import java.util.EventListener;

@Deprecated
public interface AppEventHandler extends EventListener {
    String id();
    void onEvent();
}
