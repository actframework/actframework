package act.job;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a method to be a job called at fixed duration. The difference between
 * fixed duration (implied by this annotation) and the fixed delay (implied
 * by {@link FixedDelay} annotation is calculation of the duration is different:
 * <p>For fixed duration (Every XX), the duration is calculated at the begining
 * of each invocation</p>
 * <p>For fixed delay, the duration is calculated at the end of each invocation,
 * thus the job execution time will impact the next time the job is invoked</p>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Every {
    /**
     * Specifies the duration to execute the method. Time could be specified in
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
     */
    String value() default "1s";

    /**
     * Specify the ID of the scheduled job. Default value: empty string
     * @return the job id
     */
    String id() default "";
}
