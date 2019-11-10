package act.crypto;

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

import act.session.JWT;
import org.osgl.util.Codec;
import org.osgl.util.E;
import org.osgl.util.S;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class HMAC {

    public enum Algorithm {
        SHA256, SHA384, SHA512
        ;
        private final String javaName;
        private final String jwtName;

        Algorithm() {
            this.javaName = S.concat("Hmac", name());
            this.jwtName = S.concat("HS", name().substring(3));
        }

        Mac macOf(String key) {
            try {
                SecretKeySpec spec = new SecretKeySpec(key.getBytes(Charset.forName("UTF-8")), javaName);
                Mac mac = Mac.getInstance(javaName);
                mac.init(spec);
                return mac;
            } catch (Exception e) {
                throw E.unexpected(e);
            }
        }

        public String jwtName() {
            return jwtName;
        }

    }

    private Mac mac;
    private String algoName;
    protected Algorithm algo;
    private final Charset UTF_8 = Charset.forName("UTF-8");

    protected HMAC(String algoKey) {
        algo = algoLookup.get(algoKey.toUpperCase());
        E.illegalArgumentIf(null == algo, "Algorithm not found");
        algoName = algo.jwtName();
    }

    protected HMAC(Algorithm algo) {
        this.algoName = algo.jwtName();
        this.algo = algo;
    }

    public HMAC(String key, String algoKey) {
        this(algoKey);
        mac = algo.macOf(key);
    }

    public HMAC(String key, Algorithm algo) {
        this(algo);
        mac = algo.macOf(key);
    }

    public String toString(JWT.Token token) {
        token.header(JWT.Header.ALGO, algoName);
        String headers = token.headerJsonString();
        String payloads = token.payloadJsonString();
        String encodedHeaders = encodePart(headers);
        String encodedPayloads = encodePart(payloads);
        StringBuilder buf = new StringBuilder(encodedHeaders)
                .append(".")
                .append(encodedPayloads);
        String hash = hash(buf.toString());
        return buf.append(".").append(hash).toString();
    }

    public String hash(String text) {
        return hash(text.getBytes(UTF_8));
    }

    public String hash(byte[] bytes) {
        byte[] hashed = doHash(bytes);
        return encodePart(hashed);
    }

    protected byte[] doHash(byte[] bytes) {
        return doHash(bytes, mac());
    }

    protected final byte[] doHash(byte[] bytes, Mac mac) {
        return mac.doFinal(bytes);
    }

    public boolean verifyHash(String content, String hash) {
        int len = hash.length();
        int padding = 4 - len % 4;
        if (padding > 0) {
            hash = S.concat(hash, S.times(Codec.URL_SAFE_BASE64_PADDING_CHAR, padding));
        }
        byte[] yourHash = Codec.decodeUrlSafeBase64(hash);
        return verifyHash(content.getBytes(UTF_8), yourHash);
    }

    protected boolean verifyHash(byte[] payload, byte[] hash) {
        return verifyHash(payload, hash, mac());
    }

    protected final boolean verifyHash(byte[] payload, byte[] hash, Mac mac) {
        byte[] myHash = doHash(payload, mac);
        return MessageDigest.isEqual(myHash, hash);
    }

    public boolean verifyArgo(String algoName) {
        Algorithm algorithm = algoLookup.get(algoName);
        return null != algorithm && S.eq(this.algoName, algorithm.jwtName());
    }

    private static final ThreadLocal<Mac> curMac = new ThreadLocal<>();
    private Mac mac() {
        Mac mac = curMac.get();
        if (null == mac) {
            mac = newMac();
            curMac.set(mac);
        }
        return mac;
    }

    private Mac newMac() {
        try {
            return (Mac) mac.clone();
        } catch (CloneNotSupportedException e) {
            throw E.unsupport("mac not clonable");
        }
    }

    private static Map<String, Algorithm> algoLookup; static {
        algoLookup = new HashMap<>();
        for (Algorithm algo : Algorithm.values()) {
            algoLookup.put(algo.javaName.toUpperCase(), algo);
            algoLookup.put(algo.name().toUpperCase(), algo);
            algoLookup.put(algo.jwtName().toUpperCase(), algo);
        }
    }

    protected static String encodePart(String part) {
        return Codec.encodeUrlSafeBase64(part);
    }
    protected static String encodePart(byte[] part) {
        return Codec.encodeUrlSafeBase64(part);
    }

}
