package act.app.event;

import act.app.App;
import act.app.AppServiceBase;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AppEventManager extends AppServiceBase<AppEventManager> {

    private ConcurrentMap<AppEvent, Channel> channelListeners = new ConcurrentHashMap<>();


    public AppEventManager(App app) {
        super(app);
    }

    @Override
    protected void releaseResources() {
        channelListeners.clear();
    }

    public AppEventManager on(AppEvent event, AppEventHandler listener) {
        Channel ch = channelListeners.get(event);
        if (null == ch) {
            Channel newCh = new Channel();
            ch = channelListeners.putIfAbsent(event, newCh);
            if (null == ch) {
                ch = newCh;
            }
        }
        ch.register(listener);
        return this;
    }

    public void emitEvent(AppEvent event) {
        synchronized (channelListeners) {
            Channel channel = channelListeners.get(event);
            if (null != channel) {
                channel.broadcast();
            }
        }
    }

    private static class Channel {
        List<AppEventHandler> listeners = C.newList();
        void register(AppEventHandler listener) {
            E.NPE(listener);
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
        void broadcast() {
            for (AppEventHandler l : listeners) {
                l.onEvent();
            }
        }
    }
}
