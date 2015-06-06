package act.job;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a method to be a job that will be executed with the
 * invocation of another job specified by the value
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface AlongWith {
    /**
     * Specifies the ID of the Job with which the annotated method will be invoked together
     */
    String value();
}
