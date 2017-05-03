package act.security;

import act.app.util.AppCrypto;
import act.conf.AppConfig;
import act.util.SingletonBase;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.util.E;
import org.osgl.util.Token;

import javax.inject.Inject;

/**
 * A simple implementation of {@link SecureTicketCodec} that process
 * `id` and `username` in session only
 */
public class UsernameSecureTicketCodec extends SingletonBase implements SecureTicketCodec {

    private String sessionKeyUsername;
    private AppCrypto crypto;

    @Inject
    public UsernameSecureTicketCodec(AppConfig config, AppCrypto crypto) {
        this.sessionKeyUsername = config.sessionKeyUsername();
        this.crypto = $.notNull(crypto);
    }

    @Override
    public String createTicket(H.Session session) {
        String id = session.id();
        String username = session.get(sessionKeyUsername);
        E.illegalStateIf(null == username, "User has not logged in yet");
        return crypto.generateToken(id, username);
    }

    @Override
    public H.Session parseTicket(String ticket) {
        H.Session session = new H.Session();
        Token tk = crypto.parseToken(ticket);
        if (tk.notValid()) {
            throw new SecureTicketException("ticket not valid");
        }
        $.setField("id", session, tk.id());
        session.put(sessionKeyUsername, tk.firstPayload());
        return session;
    }
}
