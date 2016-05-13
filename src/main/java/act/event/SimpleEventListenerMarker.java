package act.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark an Annotation could be used to annotate a
 * method to be simple event listener.
 *
 * An annotation marked with this annotation must have
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.ANNOTATION_TYPE)
public @interface SimpleEventListenerMarker {

    /**
     * Specify parameter type list
     * @return a list of parameter types, or an empty array if the
     *         listener method shall not take any parameter
     */
    Class[] value() default {};
}
