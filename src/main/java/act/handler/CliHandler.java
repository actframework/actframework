package act.handler;

import act.Act;
import act.Destroyable;
import act.app.CliContext;
import org.osgl.$;

/**
 * Defines a thread-save function object that can be applied
 * to a {@link CliContext} context to
 * produce certain output which could be applied to cli
 * associated with the context
 */
public interface CliHandler extends $.Function<CliContext, Void>, Destroyable {
    /**
     * Invoke handler upon a cli context
     *
     * @param context the cli context
     */
    void handle(CliContext context);

    /**
     * @param commandName command name
     * @return help message
     */
    String help(String commandName);


    /**
     * Check if this handler applied in a specific {@link act.Act.Mode}
     * @return {@code true} if this handler applied in the mode, or {@code false} otherwise
     */
    boolean appliedIn(Act.Mode mode);
}
