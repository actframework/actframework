package act.security;

import act.Act;
import act.app.util.AppCrypto;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.util.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implement {@link SecureTicketCodec} on {@link String} typed ticket
 */
public class StringSecureTicketCodec extends SecureTicketCodec.Base<String> {

    private AppCrypto crypto;

    public StringSecureTicketCodec() {
        this(Act.crypto());
    }

    public StringSecureTicketCodec(Collection<String> keys) {
        this(Act.crypto(), keys);
    }

    public StringSecureTicketCodec(String... keys) {
        this(Act.crypto(), keys);
    }

    public StringSecureTicketCodec(String keys) {
        this(Act.crypto(), keys);
    }

    public StringSecureTicketCodec(AppCrypto crypto) {
        this.crypto = $.notNull(crypto);
    }

    public StringSecureTicketCodec(AppCrypto crypto, Collection<String> keys) {
        super(keys);
        this.crypto = $.notNull(crypto);
    }

    public StringSecureTicketCodec(AppCrypto crypto, String... keys) {
        super(keys);
        this.crypto = $.notNull(crypto);
    }

    public StringSecureTicketCodec(AppCrypto crypto, String keys) {
        super(keys);
        this.crypto = $.notNull(crypto);
    }

    @Override
    protected String serialize(String id, Map<String, String> payload) {
        return crypto.generateToken(id, encode(payload));
    }

    @Override
    protected Map<String, String> deserialize(String ticket, Osgl.Var<String> id) {
        Token token = crypto.parseToken(ticket);
        E.illegalArgumentIf(token.notValid(), S.concat("Invalid secure ticket: ", ticket));
        id.set(token.id());
        Map<String, String> map = new HashMap<>();
        List<String> payload = token.payload();
        for (String item : payload) {
            $.T2<String, String> t2 = decode(item);
            map.put(t2._1, Codec.decodeUrl(t2._2));
        }
        return map;
    }

    private String[] encode(Map<String, String> map) {
        int len = map.size();
        String[] sa = new String[len];
        int i = 0;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sa[i++] = encode(entry);
        }
        return sa;
    }

    private String encode(Map.Entry<String, String> entry) {
        return S.concat(entry.getKey(), "=", Codec.encodeUrl(entry.getValue()));
    }

    private $.T2<String, String> decode(String payloadItem) {
        int pos = payloadItem.indexOf('=');
        if (pos < 0) {
            throw new IllegalArgumentException(S.concat("Invalid payload item: ", payloadItem));
        }
        return $.T2(payloadItem.substring(0, pos), payloadItem.substring(pos + 1, payloadItem.length()));
    }
}
