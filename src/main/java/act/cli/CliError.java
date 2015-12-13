package act.cli;

import org.osgl.exception.FastRuntimeException;

public class CliError extends FastRuntimeException {
    public CliError(String message) {
        super(message);
    }

    public CliError(String message, Object... args) {
        super(message, args);
    }

    public CliError(Throwable cause) {
        super(cause);
    }

    public CliError(Throwable cause, String message, Object... args) {
        super(cause, message, args);
    }
}
