package act.cli;

import org.osgl.exception.FastRuntimeException;

public class CliException extends FastRuntimeException {
    public CliException(String message) {
        super(message);
    }

    public CliException(String message, Object... args) {
        super(message, args);
    }

    public CliException(Throwable cause) {
        super(cause);
    }

    public CliException(Throwable cause, String message, Object... args) {
        super(cause, message, args);
    }
}
