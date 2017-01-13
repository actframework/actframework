package act.event;

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
     * event hander:
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
    public @interface Marker {
    }

}
