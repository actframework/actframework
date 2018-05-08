package act.ws;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.Act;
import act.crypto.AppCrypto;
import org.osgl.$;
import org.osgl.util.Codec;
import org.osgl.util.S;
import org.osgl.util.Token;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Implement {@link SecureTicketCodec} on {@link String} typed ticket
 */
public class StringSecureTicketCodec extends SecureTicketCodec.Base<String> {

    public static final String MARKER = "stk:";
    private static final int MARKER_LEN = MARKER.length();

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
        this.crypto = $.requireNotNull(crypto);
    }

    public StringSecureTicketCodec(AppCrypto crypto, Collection<String> keys) {
        super(keys);
        this.crypto = $.requireNotNull(crypto);
    }

    public StringSecureTicketCodec(AppCrypto crypto, String... keys) {
        super(keys);
        this.crypto = $.requireNotNull(crypto);
    }

    public StringSecureTicketCodec(AppCrypto crypto, String keys) {
        super(keys);
        this.crypto = $.requireNotNull(crypto);
    }

    @Override
    protected String serialize(String id, Map<String, String> payload) {
        String tk = crypto.generateToken(id, encode(payload));
        return S.concat(MARKER, tk);
    }

    @Override
    protected String deserialize(String ticket, Map<String, String> payload) {
        if (ticket.startsWith(MARKER)) {
            ticket = ticket.substring(MARKER_LEN);
        }
        Token token = crypto.parseToken(ticket);
        if (token.notValid()) {
            return null;
        }
        List<String> payloadList = token.payload();
        for (String item : payloadList) {
            $.T2<String, String> t2 = decode(item);
            payload.put(t2._1, Codec.decodeUrl(t2._2));
        }
        return token.id();
    }

    @Override
    public boolean probeTicket(Object ticket) {
        String s = ticket.toString();
        return s.startsWith(MARKER);
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
