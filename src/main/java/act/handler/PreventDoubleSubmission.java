package act.handler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark an action handler method to be double-submission protected
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PreventDoubleSubmission {

    public static final String DEFAULT = "--configured--";

    /**
     * Specify the dsp (double submission protection) token name
     * @return the dsp token name
     */
    String value() default  DEFAULT;
}
