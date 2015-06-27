package act.app.event;

import act.app.App;
import act.app.AppServiceBase;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.List;
import java.util.Map;

public class AppEventManager extends AppServiceBase<AppEventManager> {

    private List<AppEventListener> listeners = C.newList();
    private List<AppEventListener> toBeAdded = C.newList();
    private Map<AppEvent, Channel> channelListeners = C.newMap();

    private boolean listenerLock = false;

    public AppEventManager(App app) {
        super(app);
    }

    @Override
    protected void releaseResources() {
        listeners.clear();
    }

    public AppEventManager register(AppEventListener listener) {
        E.NPE(listener);
        synchronized (this) {
            if (listenerLock) {
                toBeAdded.add(listener);
            } else {
                if (!listeners.contains(listener)) {
                    listeners.add(listener);
                }
            }
        }
        return this;
    }

    public AppEventManager listenTo(AppEvent event, EventChannelListener listener) {
        Channel ch = channelListeners.get(event);
        if (null == ch) {
            ch = new Channel();
            channelListeners.put(event, ch);
        }
        ch.register(listener);
        return this;
    }

    public void emitEvent(AppEvent event) {
        synchronized (this) {
            listenerLock = true;
            try {
                Channel channel = channelListeners.get(event);
                if (null != channel) {
                    channel.broadcast();
                }
                for (AppEventListener listener : listeners) {
                    listener.handleAppEvent(event);
                }
                for (AppEventListener listener : toBeAdded) {
                    if (!listeners.contains(listener)) {
                        listeners.add(listener);
                    }
                }
                toBeAdded.clear();
            } finally {
                listenerLock = false;
            }
        }
    }

    private static class Channel {
        List<EventChannelListener> listeners = C.newList();
        void register(EventChannelListener listener) {
            E.NPE(listener);
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
        void broadcast() {
            for (EventChannelListener l : listeners) {
                l.onEvent();
            }
        }
    }
}
