package act.cli;

import org.osgl.exception.InvalidArgException;

public class InvalidCommandLineException extends InvalidArgException {
    public InvalidCommandLineException(String message) {
        super(message);
    }

    public InvalidCommandLineException(String message, Object... args) {
        super(message, args);
    }
}
