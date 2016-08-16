package act.util;

import act.app.event.AppEventId;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation is used on a certain method to mark it
 * as a callback method when a certain class has been found
 * by super class specified.
 *
 * The eligible method signature of sub class finder is
 *
 * ```java
 * @SubClassFinder
 * public void foo(Class&lt;TYPE&gt;) {...}
 * ```
 *
 * Where `foo` could be any valid Java method name
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubClassFinder {

    String DEF_VALUE = SubClassFinder.class.getName();

    /**
     * Specify the "What" to find the class, i.e. the super class
     * of the target classes to be found.
     *
     *
     * If value is not specified, then Actframework will get the `What`
     * information from the method signature
     *
     * @return the super class used to find the target classes
     */
    Class<?> value() default SubClassFinder.class;

    /**
     * Specify when to execute the call back for a certain found class.
     * <p>
     * By default the value of `callOn` is {@link AppEventId#DEPENDENCY_INJECTOR_PROVISIONED}
     *
     * @return the "When" to execute the callback logic
     */
    AppEventId callOn() default AppEventId.DEPENDENCY_INJECTOR_PROVISIONED;

}
