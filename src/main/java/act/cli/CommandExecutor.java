package act.cli;

import act.app.CliContext;
import act.util.DestroyableBase;

/**
 * A command executor execute a command and return the result
 */
public abstract class CommandExecutor extends DestroyableBase {
    /**
     * Execute the command within the {@link CliContext context} specified
     * @param context the CLI context
     * @return the execution result
     */
    public abstract Object execute(CliContext context);
}
