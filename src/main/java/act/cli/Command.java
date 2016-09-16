package act.cli;

import act.Act;

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

    /**
     * Specify the ActFramework working mode this command is bind to.
     * <p>
     *     Note {@code DEV} mode command will not available at {@code PROD} mode.
     *     However {@code PROD} mode command is available at {@code DEV} mode
     * </p>
     * @return the actframework working mode
     */
    Act.Mode mode() default Act.Mode.PROD;

    class Util {

    }
}
