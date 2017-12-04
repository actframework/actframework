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
import act.util.Lazy;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.util.E;
import org.osgl.util.S;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;

import static org.osgl.http.H.Session.KEY_EXPIRATION;

@Singleton
@Lazy
public class JsonWebTokenSessionCodec implements SessionCodec {

    private final Algorithm algorithm;
    private final boolean sessionWillExpire;
    private final int ttl;
    private final String pingPath;

    @Inject
    public JsonWebTokenSessionCodec(AppConfig conf) {
        try {
            algorithm = Algorithm.HMAC256(conf.secret());
        } catch (UnsupportedEncodingException e) {
            throw E.unexpected(e);
        }
        ttl = conf.sessionTtl() * 1000;
        sessionWillExpire = ttl > 0;
        pingPath = conf .pingPath();
    }

    @Override
    public String encodeSession(H.Session session) {
        if (null == session) {
            return null;
        }
        boolean sessionChanged = session.changed();
        if (!sessionChanged && (session.empty() || !sessionWillExpire)) {
            // Nothing changed and no cookie-expire or empty, consequently send nothing back.
            return null;
        }
        session.id(); // ensure session ID is generated
        if (sessionWillExpire && !session.contains(KEY_EXPIRATION)) {
            // session get cleared before
            session.put(KEY_EXPIRATION, $.ms() + ttl);
        }
        return builder(session, JWT.create().withJWTId(session.id())).sign(algorithm);
    }

    @Override
    public String encodeFlash(H.Flash flash) {
        if (null == flash || flash.isEmpty()) {
            return null;
        }
        return builder(flash, JWT.create()).sign(algorithm);
    }

    @Override
    public H.Session decodeSession(String encodedSession, H.Request request) {
        H.Session session = new H.Session();
        boolean newSession = true;
        if (S.notBlank(encodedSession)) {
            resolveFromJwtToken(session, encodedSession, true);
            newSession = false;
        }
        DefaultSessionCodec.processExpiration(
                session, $.ms(), newSession,
                sessionWillExpire, ttl, pingPath,
                request);
        return session;
    }

    @Override
    public H.Flash decodeFlash(String encodedFlash) {
        H.Flash flash = new H.Flash();
        if (S.notBlank(encodedFlash)) {
            resolveFromJwtToken(flash, encodedFlash, false);
            flash.discard(); // prevent cookie content from been output to response again
        }
        return flash;
    }

    private JWTCreator.Builder builder(H.KV<?> state, JWTCreator.Builder builder) {
        for (String k : state.keySet()) {
            String v = state.get(k);
            if (H.Session.KEY_EXPIRATION.equals(k)) {
                long l = Long.parseLong(v);
                builder.withExpiresAt(new Date(l));
            } else if (H.Session.KEY_ID.equals(k)) {
                // ignore this
            } else {
                builder.withClaim(k, v);
            }
        }
        return builder.withIssuer("act");
    }

    private void resolveFromJwtToken(H.KV<?> state, String token, boolean isSession) {
        JWTVerifier verifier = JWT.require(algorithm).withIssuer("act").build();
        try {
            DecodedJWT jwt = verifier.verify(token);
            Map<String, Claim> claims = jwt.getClaims();
            for (Map.Entry<String, Claim> entry : claims.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue().asString();
                if ("jti".equals(key)) {
                    if (isSession) {
                        state.put(H.Session.KEY_ID, val);
                    }
                } else if ("exp".equals(key)) {
                    state.put(H.Session.KEY_EXPIRATION, entry.getValue().asLong());
                } else if ("iss".equals(key)) {
                    // ignore
                } else {
                    state.put(key, val);
                }
            }
        } catch (JWTVerificationException e) {
            // ignore
        }
    }


}
