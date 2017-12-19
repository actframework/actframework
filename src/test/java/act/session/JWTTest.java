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

import act.crypto.HMAC;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.Before;
import org.junit.Test;
import org.osgl.$;
import org.osgl.util.S;
import osgl.ut.TestBase;

import java.util.Date;

import static act.session.JWT.Payload.EXPIRES_AT;
import static act.session.JWT.Payload.JWT_ID;

public class JWTTest extends TestBase {

    private final String SECRET = S.random(12);
    private final String ISSUER = "act-ut";
    private final String KEY_USERNAME = "username";
    private final String USERNAME = "tom@gmail.com";
    private final int EXPIRE_AT = (int) (($.ms() + 1000 * 60 * 30) / 1000);
    private final String TOKEN_ID = S.uuid();

    private JWT.Token token;
    private JWT jwt;
    private String encoded;

    @Before
    public void setup() {
        token = new JWT.Token(ISSUER);
        token.payload(KEY_USERNAME, USERNAME);
        token.payload(EXPIRES_AT, EXPIRE_AT);
        token.payload(JWT_ID, TOKEN_ID);
        HMAC hmac = new HMAC(SECRET, HMAC.Algorithm.SHA256);
        jwt = new JWT(hmac, ISSUER);
        encoded = jwt.serialize(token);
    }

    @Test
    public void testActEncodeDecode() {
        JWT.Token decoded = jwt.deserialize(encoded);
        eq(token, decoded);
    }

    @Test
    public void testDecodeAuth0() throws Exception {
        String encoded = fromAuth0();
        JWT.Token decoded = jwt.deserialize(encoded);
        eq(token, decoded);
    }

    private String fromAuth0() throws Exception {
        JWTCreator.Builder builder = com.auth0.jwt.JWT.create();
        builder.withIssuer(ISSUER);
        builder.withExpiresAt(new Date(EXPIRE_AT * 1000l));
        builder.withJWTId(TOKEN_ID);
        builder.withClaim(KEY_USERNAME, USERNAME);
        Algorithm algorithm = Algorithm.HMAC256(SECRET);
        return builder.sign(algorithm);
    }

}
