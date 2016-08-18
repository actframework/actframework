package act.inject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate binding from a {@link org.osgl.http.H.Header} to a controller field or action handler
 * parameter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface HeaderVariable {

    /**
     * The HTTP header name
     *
     * Default value: "", meaning it shall
     * use the field name or parameter name to
     * get the session attribute value
     */
    String value() default "";
}
