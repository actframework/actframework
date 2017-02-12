package act.inject.param;


import java.lang.annotation.*;

/**
 * Mark a field that should not participate
 * in the parameter value loading process
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
public @interface NoBind {
}
