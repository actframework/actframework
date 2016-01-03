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
     * alias of {@link #name()}
     * @return the command string
     */
    String value() default "";

    /**
     * Returns the name of the command
     * @return the command string
     */
    String name() default "";

    /**
     * @return the help message for the command
     */
    String help() default "";

    public static class Util {

    }
}
