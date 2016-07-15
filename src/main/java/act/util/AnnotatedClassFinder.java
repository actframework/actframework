package act.util;

import act.app.event.AppEventId;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation is used on a certain method to mark it
 * as a callback method when a certain class has been found
 * by annotation class specified
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AnnotatedClassFinder {

    /**
     * Specify the "What" to find the class, i.e. the annotation
     * class that has been used to tag the target class
     *
     * @return the annotation class used to find the target classes
     */
    Class<?> value();

    /**
     * Specify when to execute the call back for a certain found class.
     * <p>
     * By default the value of `callOn` is {@link AppEventId#DEPENDENCY_INJECTOR_PROVISIONED}
     *
     * @return the "When" to execute the callback logic
     */
    AppEventId loadOn() default AppEventId.DEPENDENCY_INJECTOR_PROVISIONED;

}
