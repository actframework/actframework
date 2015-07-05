package act.event;

import act.app.App;
import act.app.AppServiceBase;
import act.app.event.*;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Deprecated
public class AppEventManager extends AppServiceBase<AppEventManager> {

    private ConcurrentMap<AppEventId, Channel> channelListeners = new ConcurrentHashMap<>();

    public final AppDbSvcLoaded APP_DB_SVC_LOADED;
    public final AppPreLoadClasses APP_PRE_LOAD_CLASSES;
    public final AppClassLoaderInitialized APP_CLASS_LOADER_INITIALIZED;
    public final AppClassLoaded APP_CLASS_LOADED;
    public final AppPreStart APP_PRE_START;
    public final AppStart APP_START;
    public final AppStop APP_STOP;

    public AppEventManager(App app) {
        super(app);
        APP_DB_SVC_LOADED = new AppDbSvcLoaded(app);
        APP_PRE_LOAD_CLASSES = new AppPreLoadClasses(app);
        APP_CLASS_LOADER_INITIALIZED = new AppClassLoaderInitialized(app);
        APP_CLASS_LOADED = new AppClassLoaded(app);
        APP_PRE_START = new AppPreStart(app);
        APP_START = new AppStart(app);
        APP_STOP = new AppStop(app);
    }

    @Override
    protected void releaseResources() {
        channelListeners.clear();
    }

    public AppEventManager on(AppEventId event, AppEventHandler listener) {
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

    public void emitEvent(AppEventId event) {
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
