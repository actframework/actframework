package act.job;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a method to be a job called at fixed delay time
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface FixedDelay {
    /**
     * Specifies the delayed time to execute the method. Time could be specified in
     * days, hours, minutes and seconds. For example:
     * <ul>
     *     <li>2d - 2 days</li>
     *     <li>3h - 3 hours</li>
     *     <li>5mn - 5 minutes</li>
     *     <li>1s - 1 second</li>
     * </ul>
     * <p>Note, combination time specification is not supported. For example
     * {@code "1d 5h 30mn"} will be considered to be illegal argument</p>
     * <p>If this option is miss specified, then the default value will be
     * set to {@code 1s} one second</p>
     * <p>The value shall not be zero or negative, otherwise IllegalArgumentException
     * will be thrown out</p>
     * <p>Default value: {@code 60s}</p>
     */
    String value() default "60s";
}
