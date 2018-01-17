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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * A SimpleEventListener interface is designed to let the framework support simple event
 * handling mechanism, which is summarized as below:
 *
 * # 1. Event Identifier
 *
 * It supports binding to event identifiers that are *NOT ONLY* {@link java.util.EventObject} typed instance:
 *
 * * A specific String value, e.g. `USER_LOGIN`
 * * A specific Enum value, e.g. `UserActivity.LOGIN`
 * * A specific Enum class, e.g. `UserActivity`
 * * A specific EventObject class, e.g. `UserEvent`
 *
 * # 2. Event listener declaring and event triggering
 *
 * It supports declaring event handler with method and annotation.
 *
 * * To declare event handler for a specific string value, it needs to use `@act.event.On` annotation
 * * To declare event handler for a specific enum value, it needs to create your own annotation
 * * To declare event handler for `Enum` class or `EventObject` handler class, it needs to use `act.event.OnEvent` annotation
 * * To trigger the event and invoke specific event handler(s), the call to `EventBus.trigger(event-identifier, ...)` method must have the parameter list matches the declared event handler method's argument list.
 * * It is possible to declare a method with varargs that matches event-identifier with any parameters passed in.
 *
 * Examples:
 *
 * ## 2.1 Event listener bind to a specific `String` value
 * <hr/>
 * To declare a method bind to a specific string value:
 *
 * ```java
 * {@literal @}On("USER_LOGIN")
 * public void handleUserLogin(User user, long timestamp) {...}
 * ```
 *
 * To trigger the event and invoke the event listener declared above:
 *
 * ```java
 * User user = ...;
 * eventBus.trigger("USER_LOGIN", user, System.currentTimeMillis());
 * ```
 *
 * **Note** the parameter list feed into `eventBus.trigger` call must comply to the event list
 * declared in the method. Otherwise, it will not invoke the method, e.g. the following event
 * triggering code will not invoke the `handleUserLogin` method declared above as it
 * missing the second argument declared `long timestamp`:
 *
 * ```java
 * User user = ...;
 * eventBus.trigger("USER_LOGIN", user);
 * ```
 *
 * ## 2.2 Event listener bind to a specific `Enum` value
 * <hr/>
 *
 * The enum type definition:
 *
 * ```java
 * public enum UserActivity {LOGIN, LOGOUT}
 * ```
 *
 * The annotation for event listener:
 *
 * ```java
 * {@literal @}Retention(RetentionPolicy.RUNTIME)
 * {@literal @}Target(ElementType.METHOD)
 * public @interface OnUserActivity {
 *     UserActivity value();
 * }
 * ```
 *
 * The event listener:
 *
 * ```java
 * {@literal @}OnUserActivity(UserActivity.LOGIN)
 * public void handleUserLogin(User user, long timestamp) {
 *      ...
 * }
 * ```
 *
 * To trigger the event and invoke the event listener declared above:
 *
 * ```java
 * User user = ...;
 * eventBus.trigger(UserActivity.LOGIN, user, System.currentTimeMillis());
 * ```
 *
 * Again the parameter in the `trigger` call must comply to the event listener
 * argument list, otherwise it will NOT invoke the event listener.
 *
 * ## 2.3 Event listener bind to an `Enum` type
 * <hr/>
 *
 * The enum type definition:
 *
 * ```java
 * public enum UserActivity {LOGIN, LOGOUT}
 * ```
 *
 * The event listener:
 *
 * ```java
 * {@literal @}OnEvent
 * public void handleUserActivity(UserActivity event, User user, long timestamp) {
 *      switch (event) {
 *      case LOGIN:
*           ...
 *      case LOGOUT:
 *          ...
 *      }
 * }
 * ```
 *
 * **Note** the difference of event handler bind to an enum value (section 2.2) and
 * an enum type (section 2.3):
 *
 * * It requires to create a custom annotation, e.g. `OnUserActivity` to support binding
 *   event listener to a specific enum value; while framework provided `act.event.OnEvent`
 *   is used to bind the listener to an enum type.
 * * The argument list in the method binding to specific enum value does not require
 *   the enum type be declared; while method binding to an enum type needs the enum
 *   type be declared as the first argument in the list.
 *
 * To trigger the event and invoke the event listener declared above:
 *
 * ```java
 * User user = ...;
 * eventBus.trigger(UserActivity.LOGIN, user, System.currentTimeMillis());
 * eventBus.trigger(UserActivity.LOGOUT, user, System.currentTimeMillis());
 * ```
 *
 * ## 2.4 Event listener bind to an `EventObject` type
 * <hr/>
 *
 * The `EventObject` or `ActEvent` type:
 *
 * ```java
 * public class UserLoginEvent extends ActEvent<User> {
 *     public UserLoginEvent(User user) {super(user);}
 * }
 * ```
 *
 * The event listener:
 *
 * ```java
 * {@literal @}OnEvent
 * public void handleUserLogin(UserLoginEvent event, long timestamp) {
 *     User user = event.source();
 *     ...
 * }
 * ```
 *
 * To trigger the event and invoke the event listener declared above:
 *
 * ```java
 * User user = ...;
 * eventBus.trigger(new UserLoginEvent(user), System.currentTimeMillis());
 * ```
 *
 * **Note**
 *
 * This interface is **NOT** to be implemented by application.
 */
public interface SimpleEventListener {
    /**
     * Invoke the SimpleEventListener.
     *
     * Note the first parameter in the argument list
     * will be the event object triggered
     *
     * @param args the parameters.
     */
    void invoke(Object... args);

    /**
     * Returns the argument type list.
     *
     * Note the first argument in the list must be the
     * event object type that triggers this
     * event listener.
     *
     * @return the argument type list
     */
    List<Class> argumentTypes();

    /**
     * Report if this is an asynchronous listener.
     *
     * Usually it shall check if there are `@Async` annotation tagged to
     * the underline method.
     *
     * @return `true` if this is an asynchronous listener.
     */
    boolean isAsync();

    /**
     * Mark a custom annotation type as event handler method marker.
     *
     * For example user can define their own event system:
     *
     * ```java
     * public enum MyEvent {
     *     USER_LOGGED_IN,
     *     USER_SIGNED_UP
     * }
     * ```
     *
     * And then user can define a custom event handler marker annotation type:
     *
     * ```java
     * {@literal @}SimpleEventListener.Marker
     * {@literal @}Retention(RententionPolicy.RUNTIME)
     * {@literal @}Target(ElementType.METHOD)
     * public @interface OnMyEvent {
     *     MyEvent[] value();
     *     boolean async() default false;
     * }
     * ```
     *
     * Then the developer can use the custom `OnMyEvent` to annotate a method to make it
     * a simple event handler:
     *
     * ```java
     * {@literal @}OnMyEvent(MyEvent.USER_SIGNED_UP)
     * public void handleUserSignUp(User user) {
     *     // do whatever logic needed for new signed up user
     * }
     * ```
     *
     *
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.ANNOTATION_TYPE)
    @interface Marker {
    }

}
