package act.handler;

import act.Destroyable;
import act.cli.CliContext;
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
}
