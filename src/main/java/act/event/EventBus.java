package act.event;

import act.Destroyable;
import act.app.App;
import act.app.AppServiceBase;
import act.app.event.AppEvent;
import act.app.event.AppEventId;
import act.app.event.AppEventListener;
import act.inject.DependencyInjectionBinder;
import act.inject.DependencyInjector;
import act.job.AppJobManager;
import org.osgl.mvc.result.Result;
import org.osgl.util.C;
import org.osgl.util.E;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static act.app.App.logger;

@ApplicationScoped
public class EventBus extends AppServiceBase<EventBus> {

    private boolean once;

    private final List[] appEventListeners;
    private final List[] asyncAppEventListeners;
    private final Map<Class<? extends EventObject>, List<ActEventListener>> actEventListeners;
    private final Map<Class<? extends EventObject>, List<ActEventListener>> asyncActEventListeners;
    private final Map<AppEventId, AppEvent> appEventLookup;
    private final Map<Object, List<SimpleEventListener>> adhocEventListeners;
    private final Map<Object, List<SimpleEventListener>> asyncAdhocEventListeners;

    private EventBus onceBus;

    private EventBus(App app, boolean once) {
        super(app);
        appEventListeners = initAppListenerArray();
        asyncAppEventListeners = initAppListenerArray();
        actEventListeners = C.newMap();
        asyncActEventListeners = C.newMap();
        appEventLookup = initAppEventLookup(app);
        adhocEventListeners = C.newMap();
        asyncAdhocEventListeners = C.newMap();
        loadDefaultEventListeners();
        if (!once) {
            onceBus = new EventBus(app, true);
            onceBus.once = true;
        }
    }

    @Inject
    public EventBus(App app) {
        this(app, false);
    }

    @Override
    protected void releaseResources() {
        if (null != onceBus) {
            onceBus.releaseResources();
        }
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
    /**
     * Alias of {@link #bind(AppEventId, AppEventListener)}
     */
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

    private EventBus _bind(final Map<Class<? extends EventObject>, List<ActEventListener>> listeners, final Class<? extends EventObject> c, final ActEventListener l, int ttl) {
        List<ActEventListener> list = listeners.get(c);
        if (null == list) {
            list = C.newList();
            listeners.put(c, list);
        }
        if (!list.contains(l)) {
            list.add(l);
            E.illegalArgumentIf(ttl < 0);
            if (ttl > 0) {
                app().jobManager().delay(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (EventBus.this) {
                            _unbind(listeners, c, l);
                        }
                    }
                }, ttl, TimeUnit.SECONDS);
            }
        }
        return this;
    }

    private EventBus _unbind(Map<Class<? extends EventObject>, List<ActEventListener>> listeners, Class<? extends EventObject> c, ActEventListener l) {
        List<ActEventListener> list = listeners.get(c);
        if (null != list) {
            list.remove(l);
        }
        return this;
    }

    public synchronized EventBus bind(Class<? extends EventObject> c, ActEventListener l) {
        Map<Class<? extends EventObject>, List<ActEventListener>> listeners = isAsync(l.getClass()) ? asyncActEventListeners : actEventListeners;
        return _bind(listeners, c, l, 0);
    }

    public synchronized EventBus once(Class<? extends EventObject> c, OnceEventListenerBase l) {
        if (null != onceBus) {
            onceBus.bind(c, l);
        } else {
            bind(c, l);
        }
        return this;
    }

    /**
     * Bind a transient event list to event with type `c`
     * @param c the target event type
     * @param l the listener
     * @param ttl the number of seconds the listener should live
     * @return this event bus instance
     */
    public EventBus bind(Class<? extends EventObject> c, ActEventListener l, int ttl) {
        Map<Class<? extends EventObject>, List<ActEventListener>> listeners = isAsync(l.getClass()) ? asyncActEventListeners : actEventListeners;
        return _bind(listeners, c, l, ttl);
    }

    public synchronized EventBus bindSync(Class<? extends EventObject> c, ActEventListener l) {
        return _bind(actEventListeners, c, l, 0);
    }

    public synchronized EventBus bindSync(final Class<? extends EventObject> c, final ActEventListener l, int ttl) {
        return _bind(actEventListeners, c, l, ttl);
    }

    public synchronized EventBus bindAsync(Class<? extends EventObject> c, ActEventListener l) {
        return _bind(asyncActEventListeners, c, l, 0);
    }

    public synchronized EventBus bindAsync(Class<? extends EventObject> c, ActEventListener l, int ttl) {
        return _bind(asyncActEventListeners, c, l, ttl);
    }

    @SuppressWarnings("unchecked")
    private boolean callOn(ActEvent e, ActEventListener l) {
        try {
            if (l instanceof OnceEventListener) {
                return ((OnceEventListener) l).tryHandle(e);
            } else {
                l.on(e);
                return true;
            }
        } catch (Result r) {
            // in case event listener needs to return a result back
            throw r;
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw E.unexpected(x, x.getMessage());
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
        Set<ActEventListener> toBeRemoved = C.newSet();
        for (final ActEventListener l : listeners) {
            if (!async) {
                boolean result = callOn(event, l);
                if (result && once) {
                    toBeRemoved.add(l);
                }
            } else {
                jobManager.now(new Runnable() {
                    @Override
                    public void run() {
                        callOn(event, l);
                    }
                });
            }
        }
        if (once && !toBeRemoved.isEmpty()) {
            listeners.removeAll(toBeRemoved);
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
        if (isDestroyed()) {
            return this;
        }
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
        if (isDestroyed()) {
            return this;
        }
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
        if (isDestroyed()) {
            return this;
        }
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
        if (isDestroyed()) {
            return this;
        }
        callOn(event, asyncActEventListeners, true);
        callOn(event, actEventListeners, false);
        if (null != onceBus) {
            onceBus.trigger(event);
        }
        return this;
    }

    public synchronized EventBus trigger(final ActEvent event) {
        return emit(event);
    }

    public synchronized EventBus emitAsync(final ActEvent event) {
        if (isDestroyed()) {
            return this;
        }
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
            Destroyable.Util.destroyAll(l, ApplicationScoped.class);
            l.clear();
        }
    }

    private void releaseActEventListeners(Map<?, List<ActEventListener>> listeners) {
        for (List<ActEventListener> l : listeners.values()) {
            Destroyable.Util.destroyAll(l, ApplicationScoped.class);
            l.clear();
        }
        listeners.clear();
    }

    private void releaseAdhocEventListeners(Map<Object, List<SimpleEventListener>> listeners) {
        for (List<SimpleEventListener> l : listeners.values()) {
            Destroyable.Util.tryDestroyAll(l, ApplicationScoped.class);
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
