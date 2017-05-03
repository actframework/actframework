package act.security;

import act.exception.ActException;

/**
 * Triggered when there are any issue found in secure ticket
 */
public class SecureTicketException extends ActException {
    public SecureTicketException(Throwable cause) {
        super(cause);
    }

    public SecureTicketException(String message, Object... args) {
        super(message, args);
    }
}
