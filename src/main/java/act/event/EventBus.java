package act.event;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.Destroyable;
import act.app.App;
import act.app.AppServiceBase;
import act.app.event.AppEvent;
import act.app.event.AppEventId;
import act.app.event.AppEventListener;
import act.event.bytecode.ReflectedSimpleEventListener;
import act.inject.DependencyInjectionBinder;
import act.inject.DependencyInjector;
import act.job.AppJobManager;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.mvc.result.Result;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class EventBus extends AppServiceBase<EventBus> {

    private static final Logger LOGGER = LogManager.get(EventBus.class);

    private boolean once;

    private final List[] appEventListeners;
    private final List[] asyncAppEventListeners;
    private final ConcurrentMap<Class<? extends EventObject>, List<ActEventListener>> actEventListeners;
    private final ConcurrentMap<Class<? extends EventObject>, List<ActEventListener>> asyncActEventListeners;
    private final ConcurrentMap<AppEventId, AppEvent> appEventLookup;
    private final ConcurrentMap<Object, List<SimpleEventListener>> adhocEventListeners;
    private final ConcurrentMap<Object, List<SimpleEventListener>> asyncAdhocEventListeners;

    private EventBus onceBus;

    private EventBus(App app, boolean once) {
        super(app, true);
        appEventListeners = initAppListenerArray();
        asyncAppEventListeners = initAppListenerArray();
        actEventListeners = new ConcurrentHashMap<>();
        asyncActEventListeners = new ConcurrentHashMap<>();
        appEventLookup = initAppEventLookup(app);
        adhocEventListeners = new ConcurrentHashMap<>();
        asyncAdhocEventListeners = new ConcurrentHashMap<>();
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
                LOGGER.warn(e, "error calling event handler");
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

    public static boolean isAsync(AnnotatedElement c) {
        Annotation[] aa = c.getAnnotations();
        for (Annotation a : aa) {
            if (a.annotationType().getName().contains("Async")) {
                return true;
            }
        }
        return false;
    }

    private synchronized EventBus _bind(final ConcurrentMap<Class<? extends EventObject>, List<ActEventListener>> listeners, final Class<? extends EventObject> c, final ActEventListener l, int ttl) {
        List<ActEventListener> list = listeners.get(c);
        if (null == list) {
            List<ActEventListener> newList = new ArrayList<>();
            list = listeners.putIfAbsent(c, newList);
            if (null == list) {
                list = newList;
            }
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

    private synchronized EventBus _unbind(Map<Class<? extends EventObject>, List<ActEventListener>> listeners, Class<? extends EventObject> c, ActEventListener l) {
        List<ActEventListener> list = listeners.get(c);
        if (null != list) {
            list.remove(l);
        }
        return this;
    }

    public EventBus bind(Class<? extends EventObject> c, ActEventListener l) {
        boolean async = isAsync(l.getClass()) || isAsync(c);
        ConcurrentMap<Class<? extends EventObject>, List<ActEventListener>> listeners = async ? asyncActEventListeners : actEventListeners;
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
        boolean async = isAsync(l.getClass()) || isAsync(c);
        ConcurrentMap<Class<? extends EventObject>, List<ActEventListener>> listeners = async ? asyncActEventListeners : actEventListeners;
        return _bind(listeners, c, l, ttl);
    }

    public EventBus bindSync(Class<? extends EventObject> c, ActEventListener l) {
        return _bind(actEventListeners, c, l, 0);
    }

    public EventBus bindSync(final Class<? extends EventObject> c, final ActEventListener l, int ttl) {
        return _bind(actEventListeners, c, l, ttl);
    }

    public EventBus bindAsync(Class<? extends EventObject> c, ActEventListener l) {
        return _bind(asyncActEventListeners, c, l, 0);
    }

    public EventBus bindAsync(Class<? extends EventObject> c, ActEventListener l, int ttl) {
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

    /**
     * Emit an internal event.
     *
     * Not to be used by app developer
     *
     * @param eventId the app event ID
     * @return this event bus
     */
    public synchronized EventBus emit(AppEventId eventId) {
        return emit(appEventLookup.get(eventId));
    }

    public synchronized EventBus emit(final AppEvent event) {
        if (isTraceEnabled()) {
            trace("emitting app event: %s", event);
        }
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
        if (isTraceEnabled()) {
            trace("emitting app event asynchronously: %s", event);
        }
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
        if (isDestroyed()) {
            return this;
        }
        callOn(event, asyncActEventListeners, false);
        callOn(event, actEventListeners, false);

        boolean isSystemEvent = event instanceof SystemEvent;
        if (!isSystemEvent) {
            Object payload = event.source();
            if (null != payload) {
                emitSync(payload.getClass(), payload);
            }
            emitSync(event.getClass(), event);
        }

        if (null != onceBus) {
            onceBus.triggerSync(event);
        }
        return this;
    }

    public EventBus triggerSync(final ActEvent event) {
        return emitSync(event);
    }

    @SuppressWarnings("unchecked")
    public EventBus emit(final ActEvent event) {
        if (isTraceEnabled()) {
            trace("emitting act event: %s", event);
        }

        if (isDestroyed()) {
            return this;
        }

        callOn(event, asyncActEventListeners, true);
        callOn(event, actEventListeners, false);

        boolean isSystemEvent = event instanceof SystemEvent;
        if (!isSystemEvent) {
            Object payload = event.source();
            if (null != payload) {
                emit(payload.getClass(), payload);
            }
            emit(event.getClass(), event);
        }

        if (null != onceBus) {
            onceBus.trigger(event);
        }
        return this;
    }

    public EventBus trigger(final ActEvent event) {
        return emit(event);
    }

    public EventBus emitAsync(final ActEvent event) {
        if (isTraceEnabled()) {
            trace("emitting act event asynchronously: %s", event);
        }

        if (isDestroyed()) {
            return this;
        }
        callOn(event, asyncActEventListeners, true);
        callOn(event, actEventListeners, true);

        boolean isSystemEvent = event instanceof SystemEvent;
        if (!isSystemEvent) {
            Object payload = event.source();
            if (null != payload) {
                emitAsync(payload.getClass(), payload);
            }
            emitAsync(event.getClass(), event);
        }

        if (null != onceBus) {
            onceBus.triggerAsync(event);
        }
        return this;
    }

    public EventBus triggerAsync(final ActEvent event) {
        return emitAsync(event);
    }

    private EventBus _bind(ConcurrentMap<Object, List<SimpleEventListener>> listeners, Object event, SimpleEventListener l) {
        List<SimpleEventListener> list = listeners.get(event);
        if (null == list) {
            List<SimpleEventListener> newList = new ArrayList<>();
            list = listeners.putIfAbsent(event, newList);
            if (null == list) {
                list = newList;
            }
        }
        if (!list.contains(l)) {
            list.add(l);
        }
        return this;
    }

    public EventBus bind(Object event, SimpleEventListener l) {
        boolean async = event instanceof Class && (isAsync((Class) event));
        async = async || ((l instanceof ReflectedSimpleEventListener) && ((ReflectedSimpleEventListener) l).isAsync());
        return _bind(async ? asyncAdhocEventListeners : adhocEventListeners, event, l);
    }

    public EventBus bindAsync(Object event, SimpleEventListener l) {
        return _bind(asyncAdhocEventListeners, event, l);
    }

    @SuppressWarnings("unchecked")
    private void callOn(SimpleEventListener l, Object ... args) {
        try {
            l.invoke(args);
        } catch (Result r) {
            // in case event listener needs to return a result back
            throw r;
        } catch (Exception x) {
            LOGGER.error(x, "Error executing event listener");
        }
    }

    private boolean callOn(List<? extends SimpleEventListener> listeners, boolean async, final Object ... args) {
        if (null == listeners) {
            return false;
        }
        AppJobManager jobManager = null;
        if (async) {
            jobManager = app().jobManager();
        }
        boolean hasListener = !listeners.isEmpty();
        if (!hasListener) {
            return false;
        }
        // copy the list to avoid ConcurrentModificationException
        listeners = C.list(listeners);
        for (final SimpleEventListener l : listeners) {
            if (!async) {
                callOn(l, args);
            } else {
                jobManager.now(new Runnable() {
                    @Override
                    public void run() {
                        l.invoke(args);
                    }
                });
            }
        }
        return true;
    }

    public void emit(Enum<?> event, Object... args) {
        emit(event.name(), args);
    }

    public void emit(Object event, Object ... args) {
        _emit(false, true, event, args);
    }

    public void emitSync(Object event, Object ... args) {
        _emit(false, false, event, args);
    }

    public void emitAsync(Object event, Object ... args) {
        _emit(true, true, event, args);
    }

    private void _emit(boolean async1, boolean async2, Object event, Object ... args) {
        boolean hit = callOn(adhocEventListeners.get(event), async1, args);
        hit = callOn(asyncAdhocEventListeners.get(event), async2, args) || hit;
        if (!hit && 0 == args.length) {
            _emit(async1, async2, event.getClass(), event);
        }
        if (null != onceBus) {
            onceBus._emit(async1, async2, event, args);
        }
    }

    public void trigger(Object event, Object ... args) {
        emit(event, args);
    }

    public void triggerAsync(Object event, Object ... args) {
        emitAsync(event, args);
    }

    private ConcurrentMap<AppEventId, AppEvent> initAppEventLookup(App app) {
        ConcurrentMap<AppEventId, AppEvent> map = new ConcurrentHashMap<>();
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
                    LOGGER.warn("Dependency injector not found");
                } else {
                    injector.registerDiBinder(event);
                }
            }
        });
    }

    private static boolean isTraceEnabled() {
        return LOGGER.isTraceEnabled();
    }

    private void trace(String msg, Object... args) {
        msg = S.fmt(msg, args);
        if (once) {
            msg = S.builder("[once]").append(msg).toString();
        }
        LOGGER.trace(msg);
    }

}
