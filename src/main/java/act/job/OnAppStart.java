package act.job;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a method to be a job that must be run after Application started up
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface OnAppStart {
    /**
     * <p>Indicate if the job should be run synchronously with application
     * or asynchronously.</p>
     * <p>Running job synchronously means the application
     * will not start servicing incoming requests until the job is finished.</p>
     * <p>Running job asynchronously means the job will start in a separate thread
     * and will not block the app from servicing incoming requests</p>
     * @return {@code true} if the annotated method shall be executed asynchronously
     *          or {@code false} if the method all be executed synchronously
     */
    boolean async() default false;
}
