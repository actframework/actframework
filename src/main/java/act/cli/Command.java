package act.cli;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a class method as a console command
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Command {
    /**
     * @return the command string
     */
    String value();

    public static class Util {

    }
}
