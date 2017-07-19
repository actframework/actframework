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

public interface SimpleEventListener {
    void invoke(Object... args);

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
     * ```
     * @SimpleEventListener.Marker
     * @Retention(RententionPolicy.RUNTIME)
     * @Target(ElementType.METHOD)
     * public @interface OnMyEvent {
     *     MyEvent[] value();
     *     boolean async() default false;
     * }
     * ```
     *
     * Finally user can use the custom `OnMyEvent` to mark a method as simple
     * event handler:
     *
     * ```java
     * @OnMyEvent(MyEvent.USER_SIGNED_UP)
     * public void handleUserSignUp(User user) {
     *     // do whatever logic needed for new signed up user
     * }
     * ```
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.ANNOTATION_TYPE)
    @interface Marker {
    }

}
