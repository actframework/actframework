package act.security;

import org.osgl.http.H;

/**
 * Encode/decode a secure ticket
 */
public interface SecureTicketCodec {

    /**
     * Generate a secure ticket from a session data
     * @param session the session data
     * @return a secure ticket
     */
    String createTicket(H.Session session);

    /**
     * Parse a secure ticket and construct a session data
     * @param ticket the secure ticket
     * @return a session data from the ticket
     */
    H.Session parseTicket(String ticket);

}
