package act.session;

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

import act.conf.AppConfig;
import act.crypto.HMAC;
import act.util.SingletonBase;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.joda.time.ReadableInstant;
import org.osgl.$;
import org.osgl.util.Codec;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class JWT extends SingletonBase {

    public static final String ID = "jti";

    public enum Header {
        ALGO("alg");
        private String key;

        Header(String key) {
            this.key = key;
        }
    }

    public enum Payload {
        SUBJECT("sub"),
        EXPIRES_AT("exp"),
        NOT_BEFORE("nbf"),
        ISSUED_AT("iat"),
        ISSUER("iss"),
        JWT_ID(ID),;
        private String key;

        Payload(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public static class Token {
        private Map<String, Object> headers = new HashMap<>();
        private Map<String, Object> payloads = new HashMap<>();

        public Token(AppConfig config) {
            this(config.jwtIssuer());
        }

        public Token(String issuer) {
            payloads.put(Payload.ISSUER.key, issuer);
            headers.put("typ", "JWT");
        }

        public Token header(Header header, String val) {
            headers.put(header.key, val);
            return this;
        }

        public Token payload(Payload payload, Object val) {
            if (val instanceof ReadableInstant) {
                val = (int) (((ReadableInstant) val).getMillis() / 1000);
            } else if (val instanceof Date) {
                val = (int) (((Date) val).getTime() / 1000);
            }

            return payload(payload.key, val);
        }

        public Token payload(String key, Object val) {
            payloads.put(key, val);
            return this;
        }

        public String headerJsonString() {
            return JSON.toJSONString(headers);
        }

        public String payloadJsonString() {
            return JSON.toJSONString(payloads);
        }

        public Map<String, Object> payloads() {
            return payloads;
        }

        @Override
        public int hashCode() {
            return $.hc(headers, payloads);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Token) {
                Token that = $.cast(obj);
                return $.eq(that.headers, this.headers) &&
                        $.eq(that.payloads, this.payloads);
            }
            return false;
        }

        public String toString(JWT jwt) {
            return jwt.serialize(this);
        }
    }

    private final HMAC hmac;
    private final String issuer;

    @Inject
    public JWT(AppConfig config) {
        hmac = config.jwtAlgo();
        issuer = config.jwtIssuer();
    }

    public JWT(HMAC hmac, String issuer) {
        this.hmac = $.requireNotNull(hmac);
        this.issuer = $.requireNotNull(issuer);
    }

    public Token newToken() {
        return new Token(issuer);
    }

    public String serialize(Token token) {
        return hmac.toString(token);
    }

    public Token deserialize(String tokenString) {
        List<String> parts = S.fastSplit(tokenString, ".");
        if (parts.size() != 3) {
            return null;
        }
        String encodedHeaders = parts.get(0);
        String encodedPayloads = parts.get(1);
        String hash = parts.get(2);

        if (!verifyHash(encodedHeaders, encodedPayloads, hash)) {
            return null;
        }

        String headerString = new String(Codec.decodeUrlSafeBase64(encodedHeaders));
        JSONObject headers = JSON.parseObject(headerString);
        if (!verifyArgo(headers)) {
            return null;
        }

        String payloadString = new String(Codec.decodeUrlSafeBase64(encodedPayloads));
        JSONObject payloads = JSON.parseObject(payloadString);
        if (!verifyIssuer(payloads)) {
            return null;
        }
        if (!verifyExpires(payloads)) {
            return null;
        }

        Token token = new Token(issuer);
        token.headers.putAll(headers);
        token.payloads.putAll(payloads);
        return token;
    }

    private boolean verifyHash(String header, String payload, String hash) {
        return hmac.verifyHash(S.concat(header, ".", payload), hash);
    }

    private boolean verifyIssuer(JSONObject payloads) {
        return S.eq(issuer, payloads.getString("iss"));
    }

    private boolean verifyExpires(JSONObject payloads) {
        Object obj = payloads.get(Payload.EXPIRES_AT.key);
        return null == obj || (obj instanceof Number && ((Number) obj).longValue() > ($.ms() / 1000));
    }

    private boolean verifyArgo(JSONObject headers) {
        return hmac.verifyArgo(headers.getString(Header.ALGO.key));
    }

}
