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
    private final Charset UTF_8 = Charset.forName("UTF-8");

    public HMAC(String key, String algoKey) {
        Algorithm algo = algoLookup.get(algoKey.toUpperCase());
        E.illegalArgumentIf(null == algo, "Algorithm not found");
        mac = algo.macOf(key);
        algoName = algo.jwtName();
    }

    public HMAC(String key, Algorithm algo) {
        mac = algo.macOf(key);
        algoName = algo.jwtName();
    }

    public String toString(JWT.Token token) {
        token.header(JWT.Header.ALGO, algoName);
        String headers = token.headerJsonString();
        String payloads = token.payloadJsonString();
        String encodedHeaders = Codec.encodeUrlSafeBase64(headers);
        String encodedPayloads = Codec.encodeUrlSafeBase64(payloads);
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
        byte[] hashed = mac.doFinal(bytes);
        return Codec.encodeUrlSafeBase64(hashed);
    }

    public boolean verifyHash(String content, String hash) {
        byte[] myHash = mac.doFinal(content.getBytes(UTF_8));
        int len = hash.length();
        int padding = 4 - len % 4;
        if (padding > 0) {
            hash = S.concat(hash, S.times('.', padding));
        }
        return MessageDigest.isEqual(myHash, Codec.decodeUrlSafeBase64(hash));
    }

    public boolean verifyArgo(String algoName) {
        Algorithm algorithm = algoLookup.get(algoName);
        return null != algorithm && S.eq(this.algoName, algorithm.jwtName());
    }

    private static Map<String, Algorithm> algoLookup; static {
        algoLookup = new HashMap<>();
        for (Algorithm algo : Algorithm.values()) {
            algoLookup.put(algo.javaName.toUpperCase(), algo);
            algoLookup.put(algo.name().toUpperCase(), algo);
            algoLookup.put(algo.jwtName().toUpperCase(), algo);
        }
    }

    public static void main(String[] args) {
        HMAC hmac = new HMAC("secret", "SHA256");
        System.out.println(hmac.hash("Hello World"));

        hmac = new HMAC("secret", "SHA384");
        System.out.println(hmac.hash("Hello World"));

        hmac = new HMAC("secret", "SHA512");
        System.out.println(hmac.hash("Hello World"));
    }

}
