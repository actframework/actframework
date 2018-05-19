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

import org.osgl.$;

import java.util.EventObject;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * An {@code ActEvent} is a generic version of {@link EventObject}
 * @param <T> the generic type (the sub class type) of the event source
 */
public class ActEvent<T> extends EventObject {

    protected static final Object SOURCE_PLACEHODER = new Object();

    private final long ts;

    private Class<? extends ActEvent<T>> eventType;

    /**
     * This constructor allows sub class to construct a Self source event, e.g.
     * the source can be the event instance itself.
     *
     * **Note** if sub class needs to use this constructor the {@link #getSource()} ()}
     * method must be overwritten
     */
    protected ActEvent() {
        super(SOURCE_PLACEHODER);
        ts = $.ms();
        eventType = $.cast(referType(getClass()));
    }

    /**
     * Construct an `ActEvent` with source instance
     * @param source The object on which the Event initially occurred.
     *               or any payload the developer want to attach to the event
     */
    public ActEvent(T source) {
        super(source);
        ts = $.ms();
        eventType = $.cast(referType(getClass()));
    }

    /**
     * Unlike the {@link Object#getClass()} method, which always return
     * the java class of the current instance. This {@code eventType()}
     * method allow a certain implementation of the class terminate the
     * return value of the method. For example, suppose you have a event
     * class {@code MyEvent} and you might have some anonymous class
     * of {@code MyEvent}. If you implement the {@code eventType()} of
     * {@code MyEvent} class as follows:
     * <pre>
     *     public class&lt;MyEvent&gt; eventType() {
     *         return MyEvent.class;
     *     }
     * </pre>
     * Then all the anonymous sub class will return the {@code MyEvent.class}
     * instead of their own class.
     * <p>This allows the ActFramework properly handle the event class registration</p>
     * @return the type of the event
     */
    public Class<? extends ActEvent<T>> eventType() {
        return eventType;
    }

    public final T source() {
        return $.cast(getSource());
    }

    /**
     * Return the timestamp of the event
     * @return the timestamp
     * @see System#currentTimeMillis()
     */
    public final long timestamp() {
        return ts;
    }

    public static Class<? extends ActEvent<?>>  typeOf(ActEvent<?> event) {
        return event.eventType();
    }

    public static Class<? extends EventObject> typeOf(EventObject eventObject) {
        if (eventObject instanceof ActEvent) {
            return ((ActEvent<?>) eventObject).eventType();
        } else {
            return referType(eventObject.getClass());
        }
    }

    private static Map<Class, Class> referTypes = new IdentityHashMap<>();

    private static Class<? extends EventObject> referType(Class<? extends EventObject> eventType) {
        Class<? extends EventObject> referType = referTypes.get(eventType);
        if (null == referType) {
            referType = eventType.isAnonymousClass() ? referType((Class<? extends EventObject>) eventType.getSuperclass()) : eventType;
            referTypes.put(eventType, referType);
        }
        return referType;
    }

}
