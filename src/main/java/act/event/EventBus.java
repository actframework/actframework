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

import act.Act;
import act.Destroyable;
import act.app.*;
import act.app.event.SysEvent;
import act.app.event.SysEventId;
import act.app.event.SysEventListener;
import act.inject.DependencyInjectionBinder;
import act.inject.DependencyInjector;
import act.inject.util.Sorter;
import act.job.JobManager;
import act.util.ClassInfoRepository;
import act.util.ClassNode;
import org.osgl.$;
import org.osgl.Lang;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * The event bus manages event binding and distribution.
 */
@ApplicationScoped
public class EventBus extends AppServiceBase<EventBus> {

    // The key to index adhoc event listeners
    private static class Key {
        private enum IdType {
            STRING, ENUM, CLASS
        }
        private static final Class[] EMPTY_ARG_LIST = new Class[0];
        // the event identifier, could be
        // 1. a specific string value
        // 2. a specific enum value
        // 3. an Enum class
        // 4. a class extends EventObject
        private Object id;
        private IdType idType;
        private Class[] argTypes;
        private boolean varargs;
        private Object[] args;
        Key(Object id, List<Class> argTypeList) {
            setId(id);
            if (null == argTypeList || argTypeList.isEmpty()) {
                this.argTypes = EMPTY_ARG_LIST;
                return;
            }
            Class<?> arg0Type = argTypeList.get(0);
            this.argTypes = convert(argTypeList);
            int varargsIdx = 1;
            if (this.id != arg0Type && !arg0Type.isInstance(id)) {
                E.illegalArgumentIf(idType == IdType.CLASS, "The first argument in the event listener argument list must be event when binding to an event class (Enum or EventObject). \n\t listener signatures: %s \n\t event: %s", argTypeList, this.id);
                varargsIdx = 0;
            }
            this.varargs = (varargsIdx + 1) == argTypeList.size() && Object[].class == argTypeList.get(varargsIdx);
        }
        Key(Object id, List<Class> argTypeList, Object[] args) {
            this(id, argTypeList, args, false);
        }
        Key(Object id, List<Class> argTypeList, Object[] args, boolean varargs) {
            setId(id);
            this.argTypes = convert(argTypeList);
            this.args = args;
            this.varargs = varargs;
        }

        private void setId(Object id) {
            this.idType = typeOf(id);
            this.id = IdType.CLASS == this.idType && !(id instanceof Class) ? id.getClass() : id;
        }

        public static Set<Key> keysOf(Object id, SimpleEventListener eventListener) {
            Set<Key> retSet = new HashSet<>();
            for (List<Class> argTypes : permutationOf(eventListener.argumentTypes())) {
                retSet.add(new Key(id, argTypes));
            }
            if (retSet.isEmpty()) {
                retSet.add(new Key(id, C.<Class>list()));
            }
            return retSet;
        }

        /**
         * Find the permutation of arg types. E.g
         *
         * Suppose a declared argTypes is:
         *
         * ```
         * (List, ISObject)
         * ```
         *
         * Then permutation of the argTypes includes:
         *
         * ```
         * (List, ISObject)
         * (ArrayList, ISObject)
         * (LinkedList, ISObject)
         * (ArrayList, SObject)
         * (LinkedList, SObject)
         * ...
         * ```
         *
         * @param argTypes
         * @return
         */
        private static Set<List<Class>> permutationOf(List<Class> argTypes) {
            int len = argTypes.size();
            if (len == 0) {
                return C.Set();
            }

            // get type candidates for each arg position
            final AppClassLoader classLoader = Act.app().classLoader();
            ClassInfoRepository repo = classLoader.classInfoRepository();
            final List<List<Class>> candidates = new ArrayList<>();
            for (int i = 0; i < len; ++i) {
                Class type = argTypes.get(i);
                final List<Class> list = new ArrayList<>();
                list.add(type);
                candidates.add(list);
                ClassNode node = repo.findNode(type);
                if (null != node) {
                    node.visitPublicSubTreeNodes(new Lang.Visitor<ClassNode>() {
                        @Override
                        public void visit(ClassNode classNode) throws Lang.Break {
                            list.add(Act.classForName(classNode.name()));
                        }
                    });
                }
            }

            // generate permutation of argTypes
            return permutationOf(candidates, candidates.size() - 1);
        }

        private static Set<List<Class>> permutationOf(List<List<Class>> candidates, int workingColumnId) {
            if (workingColumnId == 0) {
                Set<List<Class>> permutations = new HashSet<>();
                for (Class c : candidates.get(0)) {
                    permutations.add(C.newList(c));
                }
                return permutations;
            } else {
                Set<List<Class>> prefixPermutations = permutationOf(candidates, workingColumnId - 1);
                List<Class> currentCandidates = candidates.get(workingColumnId);
                Set<List<Class>> retSet = new HashSet<>();
                for (List<Class> argList : prefixPermutations) {
                    for (Class type : currentCandidates) {
                        List<Class> merged = C.newList(argList);
                        merged.add(type);
                        retSet.add(merged);
                    }
                }
                return retSet;
            }
        }

        static IdType typeOf(Object id) {
            if (id instanceof String) {
                return IdType.STRING;
            } else if (Enum.class.isInstance(id)) {
                return IdType.ENUM;
            } else {
                Class<?> type = id instanceof Class ? (Class<?>) id : id.getClass();
                if (Enum.class.isAssignableFrom(type) || EventObject.class.isAssignableFrom(type)) {
                    return IdType.CLASS;
                } else {
                    throw E.unexpected("Invalid event type: %s", id);
                }
            }
        }

        private static Class[] convert(List<Class> argList) {
            int sz = argList.size();
            Class[] ca = argList.toArray(new Class[sz]);
            for (int i = 0; i < sz; ++i) {
                ca[i] = $.wrapperClassOf(ca[i]);
            }
            return ca;
        }

        private static Class<?> VARARG_TYPE = Object[].class;

        private static ConcurrentMap<Class, Class> typeMap = new ConcurrentHashMap<>();
        static {
            typeMap.put(ArrayList.class, List.class);
            typeMap.put(LinkedList.class, List.class);
            typeMap.put($.Val.class, List.class);
            typeMap.put($.Var.class, List.class);
            typeMap.put(HashSet.class, Set.class);
            typeMap.put(TreeSet.class, Set.class);
            typeMap.put(LinkedHashSet.class, Set.class);
            typeMap.put(HashMap.class, Map.class);
            typeMap.put(LinkedHashMap.class, Map.class);
            typeMap.put(ConcurrentHashMap.class, Map.class);
        }

        // checkout https://github.com/actframework/actframework/issues/518
        private static Class effectiveTypeOf(Object o) {
            return effectiveTypeOf(o.getClass());
        }

        private static Class effectiveTypeOf(Class<?> type) {
            if (null == type || Object.class == type) {
                return type;
            }
            Class mappedType = typeMap.get(type);
            if (null == mappedType) {
                int modifiers = type.getModifiers();
                if (!Modifier.isPublic(modifiers)
                        || type.isAnonymousClass()
                        || type.isLocalClass()
                        || type.isMemberClass()) {
                    boolean isCollection = Collection.class.isAssignableFrom(type);
                    boolean isMap = !isCollection && Map.class.isAssignableFrom(type);
                    if (isCollection || isMap) {
                        if (isMap) {
                            typeMap.putIfAbsent(type, Map.class);
                        } else if (List.class.isAssignableFrom(type)) {
                            typeMap.putIfAbsent(type, List.class);
                        } else if (Set.class.isAssignableFrom(type)) {
                            typeMap.putIfAbsent(type, Set.class);
                        }
                        mappedType = typeMap.get(type);
                    }
                    if (null == mappedType) {
                        Class[] ca = type.getInterfaces();
                        if (ca.length > 0) {
                            for (Class intf : ca) {
                                if (Modifier.isPublic(intf.getModifiers())) {
                                    mappedType = intf;
                                }
                            }
                        }
                    }
                    if (null == mappedType) {
                        Class<?> parent = type.getSuperclass();
                        mappedType = (null == parent || Object.class == parent) ? type : effectiveTypeOf(parent);
                    }
                    typeMap.putIfAbsent(type, mappedType);
                } else {
                    if (List.class.isAssignableFrom(type)) {
                        typeMap.putIfAbsent(type, List.class);
                    } else if (Set.class.isAssignableFrom(type)) {
                        typeMap.putIfAbsent(type, Set.class);
                    } else if (Map.class.isAssignableFrom(type)) {
                        typeMap.putIfAbsent(type, Map.class);
                    } else {
                        typeMap.putIfAbsent(type, type);
                    }
                    mappedType = typeMap.get(type);
                }
            }
            return mappedType;
        }

        // create list of keys from event triggering id and argument list
        static List<Key> keysOf(Class<?> idClass, Object id, Object[] args, EventBus eventBus) {
            List<Key> keys = new ArrayList<>();
            List<Class> argTypes = new ArrayList<>();
            List<Class> varArgTypes = new ArrayList<>();
            varArgTypes.add(VARARG_TYPE);
            for (Object arg: args) {
                if (null == arg) {
                    argTypes = null;
                    break;
                }
                argTypes.add(effectiveTypeOf(arg));
            }
            IdType type = typeOf(id);
            if (IdType.STRING == type) {
                if (!eventBus.stringsWithAdhocListeners.contains(id)) {
                    return C.list();
                }
                if (null != argTypes) {
                    keys.add(new Key(id, argTypes, args));
                }
                keys.add(new Key(id, varArgTypes, new Object[]{args}));
            } else if (IdType.CLASS == type) {
                if (!eventBus.classesWithAdhocListeners.contains(idClass)) {
                    return C.list();
                }
                varArgTypes.add(0, idClass);
                Object[] varArgs = new Object[2];
                varArgs[0] = id;
                varArgs[1] = args;
                keys.add(new Key(id, varArgTypes, varArgs));
                if (null != argTypes) {
                    Object[] finalArgs = $.concat(new Object[]{id}, args);
                    argTypes.add(0, idClass);
                    keys.add(new Key(id, argTypes, finalArgs));
                }
            } else {
                // the enum value
                if (eventBus.enumsWithAdhocListeners.contains(id)) {
                    keys.add(new Key(id, varArgTypes, new Object[]{args}));
                    if (null != argTypes) {
                        keys.add(new Key(id, argTypes, args));
                    }
                }
                // the enum class
                if (eventBus.classesWithAdhocListeners.contains(id.getClass())) {
                    List<Class> varArgTypesForClass = new ArrayList<>(2);
                    Object[] varArgs = new Object[2];
                    varArgs[0] = id;
                    varArgs[1] = args;
                    varArgTypesForClass.add(id.getClass());
                    varArgTypesForClass.add(VARARG_TYPE);
                    keys.add(new Key(id.getClass(), varArgTypesForClass, varArgs, true));
                    if (null != argTypes) {
                        List<Class> argTypesForClass = new ArrayList<>(1 + argTypes.size());
                        argTypesForClass.add(id.getClass());
                        argTypesForClass.addAll(argTypes);
                        keys.add(new Key(id.getClass(), argTypesForClass, $.concat(new Object[]{id}, args), true));
                    }
                }
            }
            return keys;
        }

        @Override
        public int hashCode() {
            return $.hc(id, argTypes);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Key) {
                Key that = $.cast(obj);
                return $.eq(that.id, this.id) && $.eq2(that.argTypes, this.argTypes);
            }
            return false;
        }

        @Override
        public String toString() {
            return !varargs ? S.fmt("(%s, %s)", id, $.toString2(argTypes)) :
                    S.fmt("(%s, ...)", id);
        }

    }

    private abstract static class EventContext<T> {
        boolean asyncForAsync = true;
        boolean asyncForSync = false;
        Class<? extends T> eventType;
        List<Key> keys;
        List<Key> keysForOnceBus;
        T event;
        Object[] args;
        EventContext(T event, Object[] args) {
            this.event = event;
            this.args = args;
        }
        EventContext(boolean asyncForAsync, boolean asyncForSync, T event, Object[] args) {
            this(event, args);
            this.asyncForAsync = asyncForAsync;
            this.asyncForSync = asyncForSync;
        }
        final Class<? extends T> eventType() {
            if (null == eventType) {
                eventType = lookupEventType();
            }
            return eventType;
        }
        List<Key> keys(EventBus eventBus) {
            if (eventBus.once) {
                if (null == keysForOnceBus) {
                    keysForOnceBus = Key.keysOf(eventType(), event, args, eventBus);
                }
                return keysForOnceBus;
            } else {
                if (null == keys) {
                    keys = Key.keysOf(eventType(), event, args, eventBus);
                }
                return keys;
            }
        }
        boolean hasArgs() {
            return 0 < args.length;
        }
        boolean shouldCallActEventListeners(EventBus eventBus) {
            return !hasArgs() && eventBus.eventsWithActListeners.contains(eventType());
        }
        Class<? extends T> lookupEventType() {
            return $.cast(event.getClass());
        }
        abstract boolean shouldCallAdhocEventListeners(EventBus eventBus);
    }

    private static class EnumEventContext extends EventContext<Enum> {
        EnumEventContext(Enum event, Object[] args) {
            super(event, args);
        }

        EnumEventContext(boolean asyncForAsync, boolean asyncForSync, Enum event, Object[] args) {
            super(asyncForAsync, asyncForSync, event, args);
        }

        @Override
        Class<? extends Enum> lookupEventType() {
            return event.getDeclaringClass();
        }

        @Override
        boolean shouldCallAdhocEventListeners(EventBus eventBus) {
            return eventBus.hasAdhocEventListenerFor(event);
        }
    }

    private static class StringEventContext extends EventContext<String> {
        StringEventContext(String event, Object[] args) {
            super(event, args);
            validateSimpleEventArgs(args);
        }

        StringEventContext(boolean asyncForAsync, boolean asyncForSync, String event, Object[] args) {
            super(asyncForAsync, asyncForSync, event, args);
            validateSimpleEventArgs(args);
        }

        @Override
        boolean shouldCallAdhocEventListeners(EventBus eventBus) {
            return eventBus.hasAdhocEventListenerFor(event);
        }

        private void validateSimpleEventArgs(Object[] args) {
            for (Object arg : args) {
                if (null == arg) {
                    throw new NullPointerException("Simple event argument cannot be null");
                }
            }
        }
    }

    private static class EventObjectContext<T extends EventObject> extends EventContext<T> {
        EventObjectContext(T event, Object[] args) {
            super(event, args);
        }

        EventObjectContext(boolean asyncForAsync, boolean asyncForSync, T event, Object[] args) {
            super(asyncForAsync, asyncForSync, event, args);
        }

        @Override
        Class<? extends T> lookupEventType() {
            return $.cast(ActEvent.typeOf(event));
        }

        @Override
        boolean shouldCallAdhocEventListeners(EventBus eventBus) {
            return eventBus.hasAdhocEventListenerFor(event);
        }
    }

    private static class ActEventContext extends EventObjectContext<ActEvent<?>> {
        ActEventContext(ActEvent<?> event, Object[] args) {
            super(event, args);
        }

        ActEventContext(boolean asyncForAsync, boolean asyncForSync, ActEvent<?> event, Object[] args) {
            super(asyncForAsync, asyncForSync, event, args);
        }

        @Override
        Class<? extends ActEvent<?>> lookupEventType() {
            return ActEvent.typeOf(event);
        }
    }

    private static final Logger LOGGER = LogManager.get(EventBus.class);

    // is this event bus for one time event listener?
    private boolean once;

    // index SysEvent by SysEventId.ordinal()
    private final SysEvent[] sysEventLookup;

    // stores sys event listeners, the listener list indexed by event ID ordinal
    private final List[] sysEventListeners;
    // stores async sys event listeners, the listener list indexed by event ID ordinal
    private final List[] asyncSysEventListeners;

    // stores the associations from event type to event listeners
    private final ConcurrentMap<Class<? extends EventObject>, List<ActEventListener>> actEventListeners;
    // stores the associations from event type to async event listeners
    private final ConcurrentMap<Class<? extends EventObject>, List<ActEventListener>> asyncActEventListeners;

    // stores the association from Key and ad hoc event listeners
    private final ConcurrentMap<Key, List<SimpleEventListener>> adhocEventListeners;
    // stores the association from Key and async ad hoc event listeners
    private final ConcurrentMap<Key, List<SimpleEventListener>> asyncAdhocEventListeners;

    // so we can quickly identify if it needs to go ahead to look for listeners
    private final Set<Class<?>> classesWithAdhocListeners = new HashSet<>();
    private final Set<Enum> enumsWithAdhocListeners = new HashSet<>();
    private final Set<String> stringsWithAdhocListeners = new HashSet<>();
    private final Set<Class<? extends EventObject>> eventsWithActListeners = new HashSet<>();

    // is this event bus for one time event listeners?
    private EventBus onceBus;

    private EventBus(App app, boolean once) {
        super(app, true);
        sysEventLookup = initSysEventLookup(app);

        sysEventListeners = initAppListenerArray();
        asyncSysEventListeners = initAppListenerArray();

        actEventListeners = new ConcurrentHashMap<>();
        asyncActEventListeners = new ConcurrentHashMap<>();

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
        releaseSysEventListeners(sysEventListeners);
        releaseSysEventListeners(asyncSysEventListeners);
        releaseActEventListeners(actEventListeners);
        releaseActEventListeners(asyncActEventListeners);
        releaseAdhocEventListeners(adhocEventListeners);
        releaseAdhocEventListeners(asyncAdhocEventListeners);
    }

    /**
     * Override parent implementation so that if this
     * event bus is a one time event bus, it will prepend
     * `[once]` into the message been logged
     *
     * @param msg
     *      the message
     * @param args
     *      the message arguments
     */
    @Override
    protected void trace(String msg, Object... args) {
        msg = S.fmt(msg, args);
        if (once) {
            msg = S.builder("[once]").append(msg).toString();
        }
        super.trace(msg);
    }

    /**
     * Bind an {@link SysEventListener} to a {@link SysEventId}.
     *
     * If `@Async` annotation is presented on the `sysEventListener`'s class,
     * it will bind the listener to the async repo, and it will be invoked
     * asynchronously if event triggered.
     *
     * **Note** this method is not supposed to be called by user application
     * directly.
     *
     * @param sysEventId
 *          the {@link SysEventId system event ID}
     * @param sysEventListener
     *      an instance of {@link SysEventListener}
     * @return this event bus instance
     */
    @SuppressWarnings("unchecked")
    public synchronized EventBus bind(final SysEventId sysEventId, final SysEventListener<?> sysEventListener) {
        boolean async = isAsync(sysEventListener.getClass());
        return _bind(async ? asyncSysEventListeners : sysEventListeners, sysEventId, sysEventListener);
    }

    /**
     * Bind an {@link ActEventListener} to an event type extended from {@link EventObject}.
     *
     * If either `eventType` or the class of `eventListener` has `@Async` annotation presented,
     * it will bind the listener into the async repo. When event get triggered the listener
     * will be invoked asynchronously.
     *
     * @param eventType
     *      the target event type - should be a sub class of {@link EventObject}
     * @param eventListener
     *      an instance of {@link ActEventListener} or it's sub class
     * @return this event bus instance
     */
    public EventBus bind(Class<? extends EventObject> eventType, ActEventListener eventListener) {
        boolean async = isAsync(eventListener.getClass()) || isAsync(eventType);
        return _bind(async ? asyncActEventListeners : actEventListeners, eventType, eventListener, 0);
    }

    /**
     * Bind an {@link ActEventListener} to an event type extended from {@link EventObject} with
     * time expiration `ttl` specified.
     *
     * If `ttl` is `0` or negative number, then the event listener will never get expired.
     *
     * If either `eventType` or the class of `eventListener` has `@Async` annotation presented,
     * it will bind the listener into the async repo. When event get triggered the listener
     * will be invoked asynchronously.
     *
     * @param eventType
     *      the target event type - should be a sub class of {@link EventObject}
     * @param eventListener
     *      an instance of {@link ActEventListener} or it's sub class
     * @param ttl
     *      the number of seconds this binding should live
     * @return this event bus instance
     */
    public EventBus bind(Class<? extends EventObject> eventType, ActEventListener eventListener, int ttl) {
        boolean async = isAsync(eventListener.getClass()) || isAsync(eventType);
        return _bind(async ? asyncActEventListeners : actEventListeners, eventType, eventListener, ttl);
    }

    /**
     * Bind a {@link SimpleEventListener} to an object. The object could be one of
     *
     * * a specific string value
     * * a specific enum value
     * * an Enum typed class
     * * an EventObject typed class
     *
     * If either `event` or the the method backed the `eventListener` has `@Async`
     * annotation presented, it will bind the listener into the async repo. When
     * event get triggered the listener will be invoked asynchronously.
     *
     * **Note** this method is not supposed to be called by user application directly.
     *
     * @param event
     *      the target event object
     * @param eventListener
     *      a {@link SimpleEventListener} instance
     * @return this event bus instance
     * @see SimpleEventListener
     */
    public EventBus bind(Object event, final SimpleEventListener eventListener) {
        return _bind(event, eventListener, eventListener.isAsync() || _isAsync(event));
    }

    /**
     * Bind an {@link SysEventListener} to a {@link SysEventId} asynchronously.
     *
     * **Note** this method is not supposed to be called by user application
     * directly.
     *
     * @param sysEventId
     *          the {@link SysEventId system event ID}
     * @param sysEventListener
     *      an instance of {@link SysEventListener}
     * @return this event bus instance
     */
    public synchronized EventBus bindAsync(SysEventId sysEventId, SysEventListener sysEventListener) {
        return _bind(asyncSysEventListeners, sysEventId, sysEventListener);
    }

    /**
     * Bind a {@link ActEventListener eventListener} to an event type extended
     * from {@link EventObject} asynchronously.
     *
     * @param eventType
     *      the target event type - should be a sub class of {@link EventObject}
     * @param eventListener
     *      the listener - an instance of {@link ActEventListener} or it's sub class
     * @return this event bus instance
     * @see #bind(Class, ActEventListener)
     */
    public EventBus bindAsync(Class<? extends EventObject> eventType, ActEventListener eventListener) {
        return _bind(asyncActEventListeners, eventType, eventListener, 0);
    }

    /**
     * Bind a {@link ActEventListener eventListener} to
     * {@link EventObject class} asynchronously with time expiration
     * specified.
     *
     * @param eventType
     *      the target event type - should be a sub class of {@link EventObject}
     * @param eventListener
     *      the listener - an instance of {@link ActEventListener} or it's sub class
     * @param ttl
     *      the number of seconds this binding should live
     * @return this event bus instance
     * @see #bind(Class, ActEventListener)
     */
    public EventBus bindAsync(Class<? extends EventObject> eventType, ActEventListener eventListener, int ttl) {
        return _bind(asyncActEventListeners, eventType, eventListener, ttl);
    }

    public EventBus bindAsync(Object event, final SimpleEventListener eventListener) {
        return _bind(event, eventListener, true);
    }


    /**
     * Bind an {@link SysEventListener} to a {@link SysEventId} synchronously.
     *
     * **Note** this method is not supposed to be called by user application
     * directly.
     *
     * @param sysEventId
     *          the {@link SysEventId system event ID}
     * @param sysEventListener
     *      an instance of {@link SysEventListener}
     * @return this event bus instance
     * @see #bind(SysEventId, SysEventListener)
     */
    public synchronized EventBus bindSync(SysEventId sysEventId, SysEventListener sysEventListener) {
        return _bind(sysEventListeners, sysEventId, sysEventListener);
    }

    /**
     * Bind a {@link ActEventListener eventListener} to an event type extended
     * from {@link EventObject} synchronously.
     *
     * @param eventType
     *      the target event type - should be a sub class of {@link EventObject}
     * @param eventListener
     *      the listener - an instance of {@link ActEventListener} or it's sub class
     * @return this event bus instance
     * @see #bind(Class, ActEventListener)
     */
    public EventBus bindSync(Class<? extends EventObject> eventType, ActEventListener eventListener) {
        return _bind(actEventListeners, eventType, eventListener, 0);
    }

    /**
     * Bind a {@link ActEventListener eventListener} to
     * {@link EventObject class} synchronously with time expiration
     * specified.
     *
     * @param eventType
     *      the target event type - should be a sub class of {@link EventObject}
     * @param eventListener
     *      the listener - an instance of {@link ActEventListener} or it's sub class
     * @param ttl
     *      the number of seconds this binding should live
     * @return this event bus instance
     * @see #bind(Class, ActEventListener)
     */
    public EventBus bindSync(final Class<? extends EventObject> eventType, final ActEventListener eventListener, int ttl) {
        return _bind(actEventListeners, eventType, eventListener, ttl);
    }
    /**
     * Emit a system event by {@link SysEventId event ID}.
     *
     * This will invoke the synchronous bound event listeners synchronously and
     * asynchronous bound event listeners asynchronously.
     *
     * **Note** this method shall not be used by application developer.
     *
     * @param eventId
     *      the {@link SysEventId system event ID}
     * @return
     *      this event bus instance
     */
    public synchronized EventBus emit(SysEventId eventId) {
        if (isDestroyed()) {
            return this;
        }
        if (null != onceBus) {
            onceBus.emit(eventId);
        }
        return _emit(true, false, eventId);
    }

    /**
     * Emit an enum event with parameters supplied.
     *
     * This will invoke all {@link SimpleEventListener} bound to the specific
     * enum value and all {@link SimpleEventListener} bound to the enum class
     * given the listeners has the matching argument list.
     *
     * For example, given the following enum definition:
     *
     * ```java
     * public enum UserActivity {LOGIN, LOGOUT}
     * ```
     *
     * We have the following simple event listener methods:
     *
     * ```java
     * {@literal @}OnEvent
     * public void handleUserActivity(UserActivity, User user) {...}
     *
     * {@literal @}OnUserActivity(UserActivity.LOGIN)
     * public void logUserLogin(User user, long timestamp) {...}
     *
     * {@literal @}OnUserActivity(UserActivity.LOGOUT)
     * public void logUserLogout(User user) {...}
     * ```
     *
     * The following code will invoke `logUserLogin` method:
     *
     * ```java
     * User user = ...;
     * eventBus.emit(UserActivity.LOGIN, user, System.currentTimeMills());
     * ```
     *
     * The `handleUserActivity` is not invoked because
     *
     * * The method parameter `(UserActivity, User, long)` does not match the declared argument list `(UserActivity, User)`
     *
     * While the following code will invoke both `handleUserActivity` and `logUserLogout` methods:
     *
     * ```java
     * User user = ...;
     * eventBus.emit(UserActivity.LOGOUT, user);
     * ```
     *
     * The `logUserLogin` method will not be invoked because
     *
     * 1. the method is bound to `UserActivity.LOGIN` enum value specifically, while `LOGOUT` is triggered
     * 2. the method has a `long timestamp` in the argument list and it is not passed to `eventBus.emit`
     *
     * @param event
     *      the target event
     * @param args
     *      the arguments passed in
     * @see SimpleEventListener
     */
    public EventBus emit(Enum<?> event, Object... args) {
        return _emitWithOnceBus(eventContext(event, args));
    }

    /**
     * Emit a string event with parameters.
     *
     * This will invoke all {@link SimpleEventListener} bound to the specified
     * string value given the listeners has the matching argument list.
     *
     * For example, suppose we have the following simple event listener methods:
     *
     * ```java
     * {@literal @}On("USER-LOGIN")
     * public void logUserLogin(User user, long timestamp) {...}
     *
     * {@literal @}On("USER-LOGIN")
     * public void checkDuplicateLoginAttempts(User user, Object... args) {...}
     *
     * {@literal @}On("USER-LOGIN")
     * public void foo(User user) {...}
     * ```
     *
     * The following code will invoke `logUserLogin` and `checkDuplicateLoginAttempts` methods:
     *
     * ```java
     * User user = ...;
     * eventBus.emit("USER-LOGIN", user, System.currentTimeMills());
     * ```
     *
     * The `foo(User)` will not invoked because:
     *
     * * The parameter list `(User, long)` does not match the declared argument list `(User)`.
     *   Here the `String` in the parameter list is taken out because it is used to indicate
     *   the event, instead of being passing through to the event handler method.
     * * The method `checkDuplicateLoginAttempts(User, Object ...)` will be invoked because
     *   it declares a varargs typed arguments, meaning it matches any parameters passed in.
     *
     * @param event
     *      the target event
     * @param args
     *      the arguments passed in
     * @see SimpleEventListener
     */
    public EventBus emit(String event, Object... args) {
        return _emitWithOnceBus(eventContext(event, args));
    }

    /**
     * Emit an event object with parameters.
     *
     * This will invoke all {@link SimpleEventListener} bound to the event object
     * class given the listeners has the matching argument list.
     *
     * If there is no parameter passed in, i.e. `args.length == 0`, then it will
     * also invoke all the {@link ActEventListener} bound to the event class.
     *
     * For example, suppose we have the following Event defined:
     *
     * ```java
     * public class UserActivityEvent extends ActEvent<User> {
     *     public UserActivityEvent(User user) {super(user);}
     * }
     * ```
     *
     * And we have the following event handler defined:
     *
     * ```java
     * {@literal @}OnEvent
     * public void logUserLogin(UserActivityEvent event, long timestamp) {...}
     *
     * {@literal @}OnEvent
     * public void checkDuplicateLoginAttempts(UserActivityEvent, Object... args) {...}
     *
     * {@literal @}OnEvent
     * public void foo(UserActivityEvent event) {...}
     * ```
     *
     * The following code will invoke `logUserLogin` and `checkDuplicateLoginAttempts` methods:
     *
     * ```java
     * User user = ...;
     * eventBus.emit(new UserActivityEvent(user), System.currentTimeMills());
     * ```
     *
     * The `foo(UserActivityEvent)` will not invoked because:
     *
     * * The parameter list `(UserActivityEvent, long)` does not match the declared
     *   argument list `(UserActivityEvent)`. Here the `String` in the parameter
     *   list is taken out because it is used to indicate the event, instead of being
     *   passing through to the event handler method.
     * * The method `checkDuplicateLoginAttempts(UserActivityEvent, Object ...)` will
     *   be invoked because it declares a varargs typed arguments, meaning it matches
     *   any parameters passed in.
     *
     * @param event
     *      the target event
     * @param args
     *      the arguments passed in
     * @see SimpleEventListener
     */
    public EventBus emit(EventObject event, Object... args) {
        return _emitWithOnceBus(eventContext(event, args));
    }

    /**
     * Overload {@link #emit(EventObject, Object...)} for performance tuning.
     * @see #emit(EventObject, Object...)
     */
    public EventBus emit(ActEvent event, Object... args) {
        return _emitWithOnceBus(eventContext(event, args));
    }

    /**
     * Emit a system event by {@link SysEventId event ID} and force event listeners
     * be invoked asynchronously without regarding to how listeners are bound.
     *
     * **Note** this method shall not be used by application developer.
     *
     * @param eventId
     *      the {@link SysEventId system event ID}
     * @return
     *      this event bus instance
     * @see #emit(SysEventId)
     */
    public synchronized EventBus emitAsync(SysEventId eventId) {
        if (isDestroyed()) {
            return this;
        }
        if (null != onceBus) {
            onceBus.emit(eventId);
        }
        return _emit(true, true, eventId);
    }

    /**
     * Emit an enum event with parameters and force all listeners to be called asynchronously.
     *
     * @param event
     *      the target event
     * @param args
     *      the arguments passed in
     * @see #emit(Enum, Object...)
     */
    public EventBus emitAsync(Enum<?> event, Object... args) {
        return _emitWithOnceBus(eventContextAsync(event, args));
    }

    /**
     * Emit a string event with parameters and force all listeners to be called asynchronously.
     *
     * @param event
     *      the target event
     * @param args
     *      the arguments passed in
     * @see #emit(String, Object...)
     */
    public EventBus emitAsync(String event, Object... args) {
        return _emitWithOnceBus(eventContextAsync(event, args));
    }

    /**
     * Emit a event object with parameters and force all listeners to be called asynchronously.
     *
     * @param event
     *      the target event
     * @param args
     *      the arguments passed in
     * @see #emit(EventObject, Object...)
     */
    public EventBus emitAsync(EventObject event, Object... args) {
        return _emitWithOnceBus(eventContextAsync(event, args));
    }

    /**
     * Overload {@link #emitAsync(EventObject, Object...)} for performance
     * tuning.
     * @see #emitAsync(EventObject, Object...)
     */
    public EventBus emitAsync(ActEvent event, Object... args) {
        return _emitWithOnceBus(eventContextAsync(event, args));
    }

    /**
     * Emit a system event by {@link SysEventId event ID} and force event listeners
     * be invoked synchronously without regarding to how listeners are bound.
     *
     * **Note** this method shall not be used by application developer.
     *
     * @param eventId
     *      the {@link SysEventId system event ID}
     * @return
     *      this event bus instance
     * @see #emit(SysEventId)
     */
    public synchronized EventBus emitSync(SysEventId eventId) {
        if (isDestroyed()) {
            return this;
        }
        if (null != onceBus) {
            onceBus.emit(eventId);
        }
        return _emit(false, false, eventId);
    }

    /**
     * Emit an enum event with parameters and force all listener to be called synchronously.
     *
     * @param event
     *      the target event
     * @param args
     *      the arguments passed in
     * @see #emit(Enum, Object...)
     */
    public EventBus emitSync(Enum<?> event, Object... args) {
        return _emitWithOnceBus(eventContextSync(event, args));
    }

    /**
     * Emit a string event with parameters and force all listener to be called synchronously.
     *
     * @param event
     *      the target event
     * @param args
     *      the arguments passed in
     * @see #emit(String, Object...)
     */
    public EventBus emitSync(String event, Object... args) {
        return _emitWithOnceBus(eventContextSync(event, args));
    }

    /**
     * Emit a event object with parameters and force all listeners to be called synchronously.
     *
     * @param event
     *      the target event
     * @param args
     *      the arguments passed in
     * @see #emit(EventObject, Object...)
     */
    public EventBus emitSync(EventObject event, Object... args) {
        return _emitWithOnceBus(eventContextSync(event, args));
    }

    /**
     * Overload {@link #emitSync(EventObject, Object...)} for performance tuning.
     * @see #emitSync(EventObject, Object...)
     */
    public EventBus emitSync(ActEvent event, Object... args) {
        return _emitWithOnceBus(eventContextSync(event, args));
    }

    /**
     * Alias of {@link #emit(SysEventId)}.
     */
    public EventBus trigger(SysEventId eventId) {
        return emit(eventId);
    }

    /**
     * Bind an {@link OnceEventListenerBase once event listener} to an {@link EventObject event object type}
     *
     * @param eventType
     *      the event object type
     * @param listener
     *      the listener
     * @return
     *      this event bus instance
     */
    public synchronized EventBus once(Class<? extends EventObject> eventType, OnceEventListenerBase listener) {
        if (null != onceBus) {
            onceBus.bind(eventType, listener);
        } else {
            bind(eventType, listener);
        }
        return this;
    }

    /**
     * Alias of {@link #emit(Enum, Object[])}.
     */
    public EventBus trigger(Enum<?> event, Object... args) {
        return emit(event, args);
    }

    /**
     * Alias of {@link #emit(String, Object[])}.
     */
    public EventBus trigger(String event, Object... args) {
        return emit(event, args);
    }

    /**
     * Alias of {@link #emit(EventObject, Object[])}.
     */
    public EventBus trigger(EventObject event, Object... args) {
        return emit(event, args);
    }

    /**
     * Alias of {@link #emit(ActEvent, Object[])}.
     */
    public EventBus trigger(ActEvent event, Object... args) {
        return emit(event, args);
    }

    /**
     * Alias of {@link #emitAsync(SysEventId)}.
     */
    public EventBus triggerAsync(SysEventId eventId) {
        return emitAsync(eventId);
    }

    /**
     * Alias of {@link #emitAsync(Enum, Object[])}.
     */
    public EventBus triggerAsync(Enum<?> event, Object... args) {
        return emitAsync(event, args);
    }

    /**
     * Alias of {@link #emitAsync(String, Object[])}.
     */
    public EventBus triggerAsync(String event, Object... args) {
        return emitAsync(event, args);
    }

    /**
     * Alias of {@link #emitAsync(EventObject, Object[])}.
     */
    public EventBus triggerAsync(EventObject event, Object... args) {
        return emitAsync(event, args);
    }

    /**
     * Alias of {@link #emitAsync(ActEvent, Object[])}.
     */
    public EventBus triggerAsync(ActEvent event, Object... args) {
        return emitAsync(event, args);
    }

    /**
     * Alias of {@link #emitSync(SysEventId)}.
     */
    public EventBus triggerSync(SysEventId eventId) {
        return emitSync(eventId);
    }

    /**
     * Alias of {@link #emitSync(Enum, Object[])}.
     */
    public EventBus triggerSync(Enum<?> event, Object... args) {
        return emitSync(event, args);
    }

    /**
     * Alias of {@link #emitSync(String, Object[])}.
     */
    public EventBus triggerSync(String event, Object... args) {
        return emitSync(event, args);
    }

    /**
     * Alias of {@link #emitSync(EventObject, Object[])}.
     */
    public EventBus triggerSync(EventObject event, Object... args) {
        return emitSync(event, args);
    }

    /**
     * Alias of {@link #emitSync(ActEvent, Object[])}.
     */
    public EventBus triggerSync(ActEvent event, Object... args) {
        return emitSync(event, args);
    }

    @SuppressWarnings("unchecked")
    private EventBus _bind(List[] listeners, SysEventId sysEventId, SysEventListener<?> l) {
        if (callNowIfEmitted(sysEventId, l)) {
            return this;
        }
        List<SysEventListener> list = listeners[sysEventId.ordinal()];
        addIntoListWithOrder(list, l);
        return this;
    }

    private synchronized EventBus _bind(final ConcurrentMap<Class<? extends EventObject>, List<ActEventListener>> listeners, final Class<? extends EventObject> eventType, final ActEventListener listener, int ttl) {
        eventsWithActListeners.add(eventType);
        List<ActEventListener> list = listeners.get(eventType);
        if (null == list) {
            List<ActEventListener> newList = new ArrayList<>();
            list = listeners.putIfAbsent(eventType, newList);
            if (null == list) {
                list = newList;
            }
        }
        if (addIntoListWithOrder(list, listener)) {
            if (ttl > 0) {
                app().jobManager().delay(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (EventBus.this) {
                            _unbind(listeners, eventType, listener);
                        }
                    }
                }, ttl, TimeUnit.SECONDS);
            }
        }
        return this;
    }

    private EventBus _bind(Object event, final SimpleEventListener eventListener, boolean async) {
        for (Key key : Key.keysOf(event, eventListener)) {
            if (key.idType == Key.IdType.CLASS) {
                Class<?> type = $.cast(key.id);
                if (EventObject.class.isAssignableFrom(type)) {
                    if (1 == key.argTypes.length) {
                        Class<? extends EventObject> eventType = $.cast(type);
                        ActEventListener<?> actEventListener = new ActEventListenerBase() {
                            @Override
                            public void on(EventObject event) {
                                eventListener.invoke(event);
                            }
                        };
                        if (async) {
                            bindAsync(eventType, actEventListener);
                        } else {
                            bindSync(eventType, actEventListener);
                        }
                        return this;
                    }
                }
                classesWithAdhocListeners.add(type);
            } else if (key.idType == Key.IdType.ENUM) {
                enumsWithAdhocListeners.add((Enum) event);
            } else {
                stringsWithAdhocListeners.add((String) event);
            }
            _bind(async ? asyncAdhocEventListeners : adhocEventListeners, key, eventListener);
        }
        return this;
    }

    private EventBus _bind(ConcurrentMap<Key, List<SimpleEventListener>> listeners, Key key, final SimpleEventListener eventListener) {
        List<SimpleEventListener> list = listeners.get(key);
        if (null == list) {
            List<SimpleEventListener> newList = new ArrayList<>();
            list = listeners.putIfAbsent(key, newList);
            if (null == list) {
                list = newList;
            }
        }
        addIntoListWithOrder(list, eventListener);
        return this;
    }

    private List<Key> _emit(boolean asyncForAsync, boolean asyncForSync, List<Key> keys, Object event, Object... args) {
        if (keys.isEmpty()) {
            return keys;
        }
        if (isTraceEnabled()) {
            String s = " ";
            if (asyncForAsync && asyncForSync) {
                s = "asynchronously";
            } else if (!asyncForAsync && !asyncForSync) {
                s = "synchronously";
            }
            trace("emitting event with parameters %s: %s %s", s, event, $.toString2(args));
        }
        for (Key key : keys) {
            _emit(key, asyncAdhocEventListeners, asyncForAsync);
            _emit(key, adhocEventListeners, asyncForSync);
        }
        return keys;
    }

    private EventBus _emit(boolean asyncForAsync, boolean asyncForSync, SysEventId eventId) {
        if (isTraceEnabled()) {
            String s = " ";
            if (asyncForAsync && asyncForSync) {
                s = "asynchronously";
            } else if (!asyncForAsync && !asyncForSync) {
                s = "synchronously";
            }
            trace("emitting system event[%s] %s", eventId, s);
        }
        SysEvent event = lookUpSysEvent(eventId);
        callOn(event, asyncSysEventListeners, true);
        callOn(event, sysEventListeners, false);
        return this;
    }

    private void _emit(Key key, ConcurrentMap<Key, List<SimpleEventListener>> listeners, boolean async) {
        final List<SimpleEventListener> list = listeners.get(key);
        if (null == list) {
            return;
        }
        final Object[] args = key.args;
        JobManager jobManager = async ? app().jobManager() : null;
        for (final SimpleEventListener listener: C.list(list)) {
            if (async) {
                jobManager.now(new Runnable() {
                    @Override
                    public void run() {
                        callOn(listener, args);
                    }
                });
            } else {
                callOn(listener, args);
            }
        }
    }

    private EventBus _emitWithOnceBus(StringEventContext context) {
        if (isDestroyed()) {
            return this;
        }
        if (null != onceBus) {
            onceBus._emitWithOnceBus(context);
        }
        if (context.shouldCallAdhocEventListeners(this)) {
            _emit(context.asyncForAsync, context.asyncForSync, context.keys(this), context.event, context.args);
        }
        return this;
    }

    private EventBus _emitWithOnceBus(EnumEventContext context) {
        if (isDestroyed()) {
            return this;
        }
        if (null != onceBus) {
            onceBus._emitWithOnceBus(context);
        }
        if (context.shouldCallAdhocEventListeners(this)) {
            _emit(context.asyncForAsync, context.asyncForSync, context.keys(this), context.event, context.args);
        }
        return this;
    }

    private EventBus _emitWithOnceBus(EventObjectContext<? extends EventObject> context) {
        if (isDestroyed()) {
            return this;
        }
        if (null != onceBus) {
            onceBus._emitWithOnceBus(context);
        }
        if (context.shouldCallActEventListeners(this)) {
            callOn(context.eventType(), context.event, asyncActEventListeners, context.asyncForAsync);
            callOn(context.eventType(), context.event, actEventListeners, context.asyncForSync);
        }
        if (context.shouldCallAdhocEventListeners(this)) {
            _emit(context.asyncForAsync, context.asyncForSync, context.keys(this), context.event, context.args);
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

    @SuppressWarnings("unchecked")
    private void callOn(SimpleEventListener l, Object[] args) {
        l.invoke(args);
    }

    @SuppressWarnings("unchecked")
    private boolean callOn(EventObject e, ActEventListener l) {
        try {
            if (l instanceof OnceEventListener) {
                return ((OnceEventListener) l).tryHandle(e);
            } else {
                l.on(e);
                return true;
            }
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw E.unexpected(x, x.getMessage());
        }
    }

    private <T extends EventObject> void callOn(final T event, List<? extends ActEventListener> listeners, boolean async) {
        if (null == listeners) {
            return;
        }
        JobManager jobManager = null;
        if (async) {
            jobManager = app().jobManager();
        }
        Set<ActEventListener> toBeRemoved = C.newSet();
        try {
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
                    }, event instanceof SysEvent);
                }
            }
        } catch (ConcurrentModificationException e) {
            String eventName;
            if (event instanceof SysEvent) {
                eventName = event.toString();
            } else {
                eventName = event.getClass().getName();
            }
            throw E.unexpected("Concurrent modification issue encountered on handling event: " + eventName);
        }
        if (once && !toBeRemoved.isEmpty()) {
            listeners.removeAll(toBeRemoved);
        }
    }

    @SuppressWarnings("unchecked")
    private void callOn(final SysEvent event, List[] sysEventListeners, boolean async) {
        List<SysEventListener> ll = sysEventListeners[event.id()];
        callOn(event, ll, async);
    }

    private <EVT extends EventObject> void callOn(Class<? extends EventObject> eventType, EVT event, Map<Class<? extends EventObject>, List<ActEventListener>> listeners, boolean async) {
        List<ActEventListener> list = listeners.get(eventType);
        callOn(event, list, async);
    }

    @SuppressWarnings("unchecked")
    private boolean callNowIfEmitted(SysEventId sysEventId, SysEventListener l) {
        if (app().eventEmitted(sysEventId)) {
            try {
                l.on(lookUpSysEvent(sysEventId));
            } catch (Exception e) {
                LOGGER.warn(e, "error calling event handler on " + sysEventId);
            }
            return true;
        }
        return false;
    }

    private ActEventContext eventContext(ActEvent<?> event, Object... args) {
        return new ActEventContext(event, args);
    }

    private EventObjectContext eventContext(EventObject event, Object... args) {
        return new EventObjectContext(event, args);
    }

    private StringEventContext eventContext(String event, Object... args) {
        return new StringEventContext(event, args);
    }

    private EnumEventContext eventContext(Enum event, Object... args) {
        return new EnumEventContext(event, args);
    }

    private ActEventContext eventContextAsync(ActEvent<?> event, Object... args) {
        return new ActEventContext(true, true, event, args);
    }

    private EventObjectContext eventContextAsync(EventObject event, Object... args) {
        return new EventObjectContext(true, true, event, args);
    }

    private StringEventContext eventContextAsync(String event, Object... args) {
        return new StringEventContext(true, true, event, args);
    }

    private EnumEventContext eventContextAsync(Enum event, Object... args) {
        return new EnumEventContext(true, true, event, args);
    }

    private ActEventContext eventContextSync(ActEvent<?> event, Object... args) {
        return new ActEventContext(false, false, event, args);
    }

    private EventObjectContext eventContextSync(EventObject event, Object... args) {
        return new EventObjectContext(false, false, event, args);
    }

    private StringEventContext eventContextSync(String event, Object... args) {
        return new StringEventContext(false, false, event, args);
    }

    private EnumEventContext eventContextSync(Enum event, Object... args) {
        return new EnumEventContext(false, false, event, args);
    }

    private SysEvent[] initSysEventLookup(App app) {
        SysEventId[] ids = SysEventId.values();
        int len = ids.length;
        SysEvent[] events = new SysEvent[len];
        for (int i = 0; i < len; ++i) {
            SysEventId id = ids[i];
            events[id.ordinal()] = id.of(app);
        }
        return events;
    }

    private List[] initAppListenerArray() {
        SysEventId[] ids = SysEventId.values();
        int len = ids.length;
        List[] l = new List[len];
        for (int i = 0; i < len; ++i) {
            l[i] = new ArrayList<>();
        }
        return l;
    }

    private void loadDefaultEventListeners() {
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

    private SysEvent lookUpSysEvent(SysEventId id) {
        return sysEventLookup[id.ordinal()];
    }

    private void releaseSysEventListeners(List[] array) {
        int len = array.length;
        for (int i = 0; i < len; ++i) {
            List<SysEventListener> l = array[i];
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

    private void releaseAdhocEventListeners(ConcurrentMap<Key, List<SimpleEventListener>> listeners) {
        for (List<SimpleEventListener> l : listeners.values()) {
            Destroyable.Util.tryDestroyAll(l, ApplicationScoped.class);
            l.clear();
        }
        listeners.clear();
    }

    private boolean hasAdhocEventListenerFor(Enum event) {
        return enumsWithAdhocListeners.contains(event) || classesWithAdhocListeners.contains(event.getDeclaringClass());
    }

    private boolean hasAdhocEventListenerFor(String event) {
        return stringsWithAdhocListeners.contains(event);
    }

    private boolean hasAdhocEventListenerFor(EventObject event) {
        return classesWithAdhocListeners.contains(event.getClass());
    }


    public static void classInit(App app) {
        Key.typeMap = app.createConcurrentMap();
    }


    public static boolean isAsync(AnnotatedElement c) {
        Annotation[] aa = c.getAnnotations();
        for (Annotation a : aa) {
            if (a.annotationType().getSimpleName().startsWith("Async")) {
                return true;
            }
        }
        return false;
    }

    private static boolean _isAsync(Object eventId) {
        return eventId instanceof Class && isAsync((Class) eventId);
    }

    private boolean addIntoListWithOrder(List list, Object element) {
        if (!list.contains(element)) {
            list.add(element);
            Collections.sort(list, Sorter.COMPARATOR);
            return true;
        }
        return false;
    }
}
