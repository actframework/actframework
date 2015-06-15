package act.exception;

/**
 * Thrown out when ACT failed to bind a http request param to action method argument
 */
public class BindException extends ActException {
    public BindException(String message) {
        super(message);
    }

    public BindException(String message, Object... args) {
        super(message, args);
    }

    public BindException(Throwable cause) {
        super(cause);
    }

    public BindException(Throwable cause, String message, Object... args) {
        super(cause, message, args);
    }
}
