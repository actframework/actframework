package act.util;

import act.app.App;
import act.app.event.AppEventId;
import org.osgl.$;
import org.osgl.util.S;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation is used on a certain method to mark it
 * as a callback method when a certain class has been found
 * by super class specified
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubClassFinder {

    /**
     * Specify the "What" to find the class, i.e. the super class
     * of the target classes to be found.
     *
     * @return the super class used to find the target classes
     */
    Class<?> value();

    /**
     * Specify when to execute the call back for a certain found class.
     * <p>
     * By default the value of `loadOn` is {@link AppEventId#DEPENDENCY_INJECTOR_PROVISIONED}
     *
     * @return the "When" to execute the callback logic
     */
    AppEventId loadOn() default AppEventId.DEPENDENCY_INJECTOR_PROVISIONED;

}
