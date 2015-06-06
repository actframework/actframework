package act.job;

import act.app.ProjectLayoutProbe;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a method to be a cron job. The method must be public and has no parameters. If
 * the method is virtual then the class declared the method must have a public constructor
 * without parameter
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Cron {
    /**
     * Specifies cron expression or a configuration key starts with {@code "cron."}
     */
    String value();
}
