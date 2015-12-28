package act.event;

import act.ActComponent;
import act.Destroyable;
import act.app.App;
import act.app.AppServiceBase;
import act.app.event.AppEvent;
import act.app.event.AppEventId;
import act.app.event.AppEventListener;
import act.job.AppJobManager;
import org.osgl.util.C;
import org.osgl.util.E;

import java.lang.annotation.Annotation;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@ActComponent
public class EventBus extends AppServiceBase<EventBus> {

    private final List[] appEventListeners;
    private final List[] asyncAppEventListeners;
    private final Map<Class<? extends EventObject>, List<ActEventListener>> actEventListeners;
    private final Map<Class<? extends EventObject>, List<ActEventListener>> asyncActEventListeners;
    private final Map<AppEventId, AppEvent> appEventLookup;

    public EventBus(App app) {
        super(app);
        appEventListeners = initAppListenerArray();
        asyncAppEventListeners = initAppListenerArray();
        actEventListeners = C.newMap();
        asyncActEventListeners = C.newMap();
        appEventLookup = initAppEventLookup(app);
    }

    @Override
    protected void releaseResources() {
        releaseAppEventListeners(appEventListeners);
        releaseAppEventListeners(asyncAppEventListeners);
        releaseActEventListeners(actEventListeners);
        releaseActEventListeners(asyncActEventListeners);
        appEventLookup.clear();
    }

    @SuppressWarnings("unchecked")
    private boolean bindIfEmitted(AppEventId appEventId, AppEventListener l) {
        if (app().eventEmitted(appEventId)) {
            try {
                l.on(appEventLookup.get(appEventId));
            } catch (Exception e) {
                logger.warn(e, "error calling event handler");
            }
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private EventBus _bind(List[] listeners, AppEventId appEventId, AppEventListener l) {
        if (bindIfEmitted(appEventId, l)) {
            return this;
        }
        List<AppEventListener> list = listeners[appEventId.ordinal()];
        if (!list.contains(l)) list.add(l);
        return this;
    }

    @SuppressWarnings("unchecked")
    public synchronized EventBus bind(AppEventId appEventId, AppEventListener l) {
        return _bind(appEventListeners, appEventId, l);
    }

    @SuppressWarnings("unused")
    public synchronized EventBus bindAsync(AppEventId appEventId, AppEventListener l) {
        return _bind(asyncAppEventListeners, appEventId, l);
    }

    @SuppressWarnings("unused")
    public synchronized EventBus bindSync(AppEventId appEventId, AppEventListener l) {
        return bind(appEventId, l);
    }

    private static boolean isAsync(Class<?> c) {
        Annotation[] aa = c.getAnnotations();
        for (Annotation a : aa) {
            if (a.annotationType().getName().contains("Async")) {
                return true;
            }
        }
        return false;
    }

    private EventBus _bind(Map<Class<? extends EventObject>, List<ActEventListener>> listeners, Class<? extends EventObject> c, ActEventListener l) {
        List<ActEventListener> list = listeners.get(c);
        if (null == list) {
            list = C.newList();
            listeners.put(c, list);
        }
        if (!list.contains(l)) list.add(l);
        return this;
    }

    public synchronized EventBus bind(Class<? extends EventObject> c, ActEventListener l) {
        Map<Class<? extends EventObject>, List<ActEventListener>> listeners = isAsync(l.getClass()) ? asyncActEventListeners : actEventListeners;
        return _bind(listeners, c, l);
    }

    public synchronized EventBus bindSync(Class<? extends EventObject> c, ActEventListener l) {
        return _bind(actEventListeners, c, l);
    }

    public synchronized EventBus bindAsync(Class<? extends EventObject> c, ActEventListener l) {
        return _bind(asyncActEventListeners, c, l);
    }

    public synchronized EventBus emit(AppEventId eventId) {
        return emit(appEventLookup.get(eventId));
    }

    public synchronized EventBus emit(final AppEvent event) {
        List<AppEventListener> ll = asyncAppEventListeners[event.id()];
        final AppJobManager jobManager = app().jobManager();
        for (final AppEventListener l : ll) {
            jobManager.now(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    l.on(event);
                    return null;
                }
            });
        }
        ll.clear();

        ll = appEventListeners[event.id()];
        for (AppEventListener l : ll) {
            try {
                l.on(event);
            } catch (Exception e) {
                logger.error(e, "Error executing job");
                throw E.tbd("support handling exception in jobs");
            }
        }
        ll.clear();
        return this;
    }

    public synchronized EventBus emitAsync(AppEventId eventId) {
        return emitAsync(appEventLookup.get(eventId));
    }

    public synchronized EventBus emitAsync(final AppEvent event) {
        List<AppEventListener> ll = asyncAppEventListeners[event.id()];
        AppJobManager jobManager = app().jobManager();
        for (final AppEventListener l : ll) {
            jobManager.now(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    l.on(event);
                    return null;
                }
            });
        }
        ll = appEventListeners[event.id()];
        for (final AppEventListener l : ll) {
            jobManager.now(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    l.on(event);
                    return null;
                }
            });
        }
        return this;
    }

    public synchronized EventBus emitSync(AppEventId eventId) {
        return emitSync(appEventLookup.get(eventId));
    }

    public synchronized EventBus emitSync(AppEvent event) {
        List<AppEventListener> ll = asyncAppEventListeners[event.id()];
        for (AppEventListener l : ll) {
            try {
                l.on(event);
            } catch (Exception e) {
                logger.error(e, "Error executing job");
                throw E.tbd("Support exception in jobs");
            }
        }
        ll = appEventListeners[event.id()];
        for (AppEventListener l : ll) {
            try {
                l.on(event);
            } catch (Exception e) {
                logger.error(e, "Error executing job");
                throw E.tbd("Support exception in jobs");
            }
        }
        return this;
    }

    public synchronized EventBus emitSync(final ActEvent event) {
        Class<? extends ActEvent> c = event.getClass();
        while (c.getName().contains("$")) {
            c = (Class) c.getSuperclass();
        }
        List<ActEventListener> list = asyncActEventListeners.get(c);
        AppJobManager jobManager = app().jobManager();
        if (null != list) {
            for (final ActEventListener l : list) {
                try {
                    l.on(event);
                } catch (Exception e) {
                    logger.error(e, "Error executing job");
                    throw E.tbd("Support exception in jobs");
                }
            }
        }
        list = actEventListeners.get(c);
        if (null != list) {
            for (ActEventListener l : list) {
                try {
                    l.on(event);
                } catch (Exception e) {
                    logger.error(e, "Error executing job");
                    throw E.tbd("Support exception in jobs");
                }
            }
        }
        return this;
    }

    public synchronized EventBus emit(final ActEvent event) {
        Class<? extends ActEvent> c = event.getClass();
//        while (c.getName().contains("$")) {
//            c = (Class)c.getSuperclass();
//        }
        List<ActEventListener> list = asyncActEventListeners.get(c);
        AppJobManager jobManager = app().jobManager();
        if (null != list) {
            for (final ActEventListener l : list) {
                jobManager.now(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        l.on(event);
                        return null;
                    }
                });
            }
        }
        list = actEventListeners.get(c);
        if (null != list) {
            for (ActEventListener l : list) {
                try {
                    l.on(event);
                } catch (Exception e) {
                    logger.error(e, "Error executing job");
                    throw E.tbd("Support exception in jobs");
                }
            }
        }
        return this;
    }

    public synchronized EventBus emitAsync(final ActEvent event) {
        Class<? extends ActEvent> c = event.getClass();
        while (c.getName().contains("$")) {
            c = (Class) c.getSuperclass();
        }
        List<ActEventListener> list = asyncActEventListeners.get(c);
        AppJobManager jobManager = app().jobManager();
        if (null != list) {
            for (final ActEventListener l : list) {
                jobManager.now(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        l.on(event);
                        return null;
                    }
                });
            }
        }
        list = actEventListeners.get(c);
        if (null != list) {
            for (final ActEventListener l : list) {
                jobManager.now(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        l.on(event);
                        return null;
                    }
                });
            }
        }
        return this;
    }

    private Map<AppEventId, AppEvent> initAppEventLookup(App app) {
        Map<AppEventId, AppEvent> map = C.newMap();
        AppEventId[] ids = AppEventId.values();
        int len = ids.length;
        for (int i = 0; i < len; ++i) {
            AppEventId id = ids[i];
            map.put(id, id.of(app));
        }
        return map;
    }

    private List[] initAppListenerArray() {
        AppEventId[] ids = AppEventId.values();
        int len = ids.length;
        List[] l = new List[len];
        for (int i = 0; i < len; ++i) {
            l[i] = C.newList();
        }
        return l;
    }

    private void releaseAppEventListeners(List[] array) {
        int len = array.length;
        for (int i = 0; i < len; ++i) {
            List<AppEventListener> l = array[i];
            Destroyable.Util.destroyAll(l);
        }
    }

    private void releaseActEventListeners(Map<?, List<ActEventListener>> listeners) {
        for (List<ActEventListener> l : listeners.values()) {
            Destroyable.Util.destroyAll(l);
        }
    }


}
