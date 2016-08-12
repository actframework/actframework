package act.inject;

import java.lang.annotation.*;

/**
 * This annotation is used mark a field or controller action
 * method parameter should be optionally populated with a
 * URL path variable value
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OptionalPathVariable {

    /**
     * Specifies the URL path variable name.
     *
     * Default value is "" meaning use the field name or
     * parameter name as the URL path variable name
     *
     * @return the path variable name
     */
    String value() default "";
}