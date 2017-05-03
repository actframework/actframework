package act.security;

import act.app.util.AppCrypto;
import act.util.SingletonBase;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.util.Codec;
import org.osgl.util.E;
import org.osgl.util.S;
import org.osgl.util.Token;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

/**
 * Default implementation of {@link SecureTicketCodec}. This implementation
 * encode everything from the session object into the secure ticket
 */
public class DefaultSecureTicketCodec extends SingletonBase implements SecureTicketCodec {

    private AppCrypto crypto;
    private String[] EMPTY_STR_ARRAY = new String[0];

    @Inject
    public DefaultSecureTicketCodec(AppCrypto crypto) {
        this.crypto = $.notNull(crypto);
    }

    @Override
    public String createTicket(H.Session session) {
        return crypto.generateToken(session.id(), toString(session));
    }

    @Override
    public H.Session parseTicket(String ticket) {
        Token tk = crypto.parseToken(ticket);
        if (tk.notValid()) {
            throw new SecureTicketException("ticket not valid");
        }
        H.Session session = createSession(tk.id());
        List<String> payload = tk.payload();
        for (String string : payload) {
            append(session, string);
        }
        return session;
    }

    private String[] toString(H.Session session) {
        Set<String> keySet = session.keySet();
        int sz = keySet.size();
        if (0 == sz) {
            return EMPTY_STR_ARRAY;
        }
        String[] retVal = new String[sz];
        S.Buffer buf = S.buffer();
        int i = 0;
        for (String key : session.keySet()) {
            buf.append(key).append("=").append(Codec.encodeUrl(session.get(key)));
            retVal[i++] = buf.toString();
            buf.reset();
        }
        return retVal;
    }

    private H.Session createSession(String id) {
        H.Session session = new H.Session();
        $.setProperty(session, id, "id");
        return session;
    }

    private void append(H.Session session, String payloadItem) {
        int pos = payloadItem.indexOf('=');
        E.illegalArgumentIf(pos < 0, "payload item not valid: " + payloadItem);
        String key = payloadItem.substring(0, pos);
        String val = payloadItem.substring(pos + 1, payloadItem.length());
        session.put(key, Codec.decodeUrl(val));
    }
}
