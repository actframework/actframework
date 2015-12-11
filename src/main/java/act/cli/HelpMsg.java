package act.cli;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a {@link Command console command} method
 * to provide the help message
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface HelpMsg {
    /**
     * @return the help message
     */
    String value();
}
