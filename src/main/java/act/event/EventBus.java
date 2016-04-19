package act.event;

import act.ActComponent;
import act.Destroyable;
import act.app.App;
import act.app.AppServiceBase;
import act.app.event.AppEvent;
import act.app.event.AppEventId;
import act.app.event.AppEventListener;
import act.di.DependencyInjectionBinder;
import act.di.DependencyInjector;
import act.job.AppJobManager;
import org.osgl.mvc.result.Result;
import org.osgl.util.C;

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
    private final Map<Object, List<SimpleEventListener>> adhocEventListeners;
    private final Map<Object, List<SimpleEventListener>> asyncAdhocEventListeners;

    public EventBus(App app) {
        super(app);
        appEventListeners = initAppListenerArray();
        asyncAppEventListeners = initAppListenerArray();
        actEventListeners = C.newMap();
        asyncActEventListeners = C.newMap();
        appEventLookup = initAppEventLookup(app);
        adhocEventListeners = C.newMap();
        asyncAdhocEventListeners = C.newMap();
        loadDefaultEventListeners();
    }

    @Override
    protected void releaseResources() {
        releaseAppEventListeners(appEventListeners);
        releaseAppEventListeners(asyncAppEventListeners);
        releaseActEventListeners(actEventListeners);
        releaseActEventListeners(asyncActEventListeners);
        releaseAdhocEventListeners(adhocEventListeners);
        releaseAdhocEventListeners(asyncAdhocEventListeners);
        appEventLookup.clear();
    }

    @SuppressWarnings("unchecked")
    private boolean callNowIfEmitted(AppEventId appEventId, AppEventListener l) {
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
        if (callNowIfEmitted(appEventId, l)) {
            return this;
        }
        List<AppEventListener> list = listeners[appEventId.ordinal()];
        if (!list.contains(l)) list.add(l);
        return this;
    }

    @SuppressWarnings("unchecked")
    public synchronized EventBus bind(final AppEventId appEventId, final AppEventListener l) {
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

    @SuppressWarnings("unchecked")
    private void callOn(ActEvent e, ActEventListener l) {
        try {
            l.on(e);
        } catch (Result r) {
            // in case event listener needs to return a result back
            throw r;
        } catch (Exception x) {
            logger.error(x, "Error executing job");
        }
    }

    private <T extends ActEvent> void callOn(final T event, List<? extends ActEventListener> listeners, boolean async) {
        if (null == listeners) {
            return;
        }
        AppJobManager jobManager = null;
        if (async) {
            jobManager = app().jobManager();
        }
        // copy the list to avoid ConcurrentModificationException
        listeners = C.list(listeners);
        for (final ActEventListener l : listeners) {
            if (!async) {
                callOn(event, l);
            } else {
                jobManager.now(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        callOn(event, l);
                        return null;
                    }
                });
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void callOn(final AppEvent event, List[] appEventListeners, boolean async) {
        List<AppEventListener> ll = appEventListeners[event.id()];
        callOn(event, ll, async);
    }

    private void callOn(ActEvent event, Map<Class<? extends EventObject>, List<ActEventListener>> listeners, boolean async) {
        List<ActEventListener> list = listeners.get(event.eventType());
        callOn(event, list, async);
    }

    public synchronized EventBus emit(AppEventId eventId) {
        return emit(appEventLookup.get(eventId));
    }

    public synchronized EventBus trigger(AppEventId eventId) {
        return emit(eventId);
    }

    public synchronized EventBus emit(final AppEvent event) {
        callOn(event, asyncAppEventListeners, true);
        callOn(event, appEventListeners, false);
        return this;
    }

    public synchronized EventBus trigger(final AppEvent event) {
        return emit(event);
    }

    public synchronized EventBus emitAsync(AppEventId eventId) {
        return emitAsync(appEventLookup.get(eventId));
    }

    public synchronized EventBus emitAsync(final AppEvent event) {
        callOn(event, asyncAppEventListeners, true);
        callOn(event, appEventListeners, true);
        return this;
    }

    public synchronized EventBus triggerAsync(final AppEvent event) {
        return emitAsync(event);
    }

    public synchronized EventBus emitSync(AppEventId eventId) {
        return emitSync(appEventLookup.get(eventId));
    }

    public synchronized EventBus triggerSync(AppEventId eventId) {
        return emitSync(eventId);
    }

    public synchronized EventBus emitSync(AppEvent event) {
        callOn(event, asyncAppEventListeners, false);
        callOn(event, appEventListeners, false);
        return this;
    }

    public synchronized EventBus triggerSync(AppEvent event) {
        return emitSync(event);
    }

    public synchronized EventBus emitSync(final ActEvent event) {
        callOn(event, asyncActEventListeners, false);
        callOn(event, actEventListeners, false);
        return this;
    }

    public synchronized EventBus triggerSync(final ActEvent event) {
        return emitSync(event);
    }

    @SuppressWarnings("unchecked")
    public synchronized EventBus emit(final ActEvent event) {
        callOn(event, asyncActEventListeners, true);
        callOn(event, actEventListeners, false);
        return this;
    }

    public synchronized EventBus trigger(final ActEvent event) {
        return emit(event);
    }

    public synchronized EventBus emitAsync(final ActEvent event) {
        callOn(event, asyncActEventListeners, true);
        callOn(event, actEventListeners, true);
        return this;
    }

    public synchronized EventBus triggerAsync(final ActEvent event) {
        return emitAsync(event);
    }

    private EventBus _bind(Map<Object, List<SimpleEventListener>> listeners, Object event, SimpleEventListener l) {
        List<SimpleEventListener> list = listeners.get(event);
        if (null == list) {
            list = C.newList();
            listeners.put(event, list);
        }
        if (!list.contains(l)) {
            list.add(l);
        }
        return this;
    }

    public synchronized EventBus bind(Object event, SimpleEventListener l) {
        return _bind(adhocEventListeners, event, l);
    }

    public synchronized EventBus bindAsync(Object event, SimpleEventListener l) {
        return _bind(asyncAdhocEventListeners, event, l);
    }

    @SuppressWarnings("unchecked")
    private void callOn(Object e, SimpleEventListener l, Object ... args) {
        try {
            l.invoke(args);
        } catch (Result r) {
            // in case event listener needs to return a result back
            throw r;
        } catch (Exception x) {
            logger.error(x, "Error executing event listener");
        }
    }

    private void callOn(Object event, List<? extends SimpleEventListener> listeners, boolean async, final Object ... args) {
        if (null == listeners) {
            return;
        }
        AppJobManager jobManager = null;
        if (async) {
            jobManager = app().jobManager();
        }
        // copy the list to avoid ConcurrentModificationException
        listeners = C.list(listeners);
        for (final SimpleEventListener l : listeners) {
            if (!async) {
                callOn(event, l, args);
            } else {
                jobManager.now(new Runnable() {
                    @Override
                    public void run() {
                        l.invoke(args);
                    }
                });
            }
        }
    }

    public synchronized void emit(Object event, Object ... args) {
        callOn(event, adhocEventListeners.get(event), false, args);
        callOn(event, asyncAdhocEventListeners.get(event), true, args);
    }

    public synchronized void emitAsync(Object event, Object ... args) {
        callOn(event, adhocEventListeners.get(event), true, args);
        callOn(event, asyncAdhocEventListeners.get(event), true, args);
    }

    public synchronized void trigger(Object event, Object ... args) {
        emit(event, args);
    }

    public synchronized void triggerAsync(Object event, Object ... args) {
        emitAsync(event, args);
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
            l.clear();
        }
    }

    private void releaseActEventListeners(Map<?, List<ActEventListener>> listeners) {
        for (List<ActEventListener> l : listeners.values()) {
            Destroyable.Util.destroyAll(l);
            l.clear();
        }
        listeners.clear();
    }

    private void releaseAdhocEventListeners(Map<Object, List<SimpleEventListener>> listeners) {
        for (List<SimpleEventListener> l : listeners.values()) {
            Destroyable.Util.tryDestroyAll(l);
            l.clear();
        }
        listeners.clear();
    }

    public void loadDefaultEventListeners() {
        loadDiBinderListener();
    }

    private void loadDiBinderListener() {
        bind(DependencyInjectionBinder.class, new ActEventListenerBase<DependencyInjectionBinder>() {
            @Override
            public void on(DependencyInjectionBinder event) throws Exception {
                DependencyInjector injector = app().injector();
                if (null == injector) {
                    logger.warn("Dependency injector not found");
                } else {
                    injector.registerDiBinder(event);
                }
            }
        });
    }


}
