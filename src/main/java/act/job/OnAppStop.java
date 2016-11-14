package act.job;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a method to be a job that must be run before App stop
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface OnAppStop {
    /**
     * <p>Indicate if the job should be run synchronously with application
     * or asynchronously.</p>
     * <p>Running job synchronously means other jobs can be executed with other jobs
     * in parallel</p>
     * <p>Running job asynchronously means the job block the application stop process
     * until it finished</p>
     * @return {@code true} if the annotated method shall be executed asynchronously
     *          or {@code false} if the method all be executed synchronously
     */
    boolean async() default false;

    /**
     * Specify the ID of the scheduled job. Default value: empty string
     * @return the job id
     */
    String id() default "";
}
