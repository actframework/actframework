package act.event;

import act.app.event.AppEventId;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a method to be an adhoc event handler
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface On {

    /**
     * Specify the event IDs
     * @return the event IDs
     */
    String[] value();

    /**
     * <p>Indicate if the handler should be run synchronously with application
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
