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

import static act.session.JWT.Payload.EXPIRES_AT;
import static act.session.JWT.Payload.ISSUER;
import static act.session.JWT.Payload.JWT_ID;
import static org.osgl.http.H.Session.KEY_EXPIRATION;

import act.conf.AppConfig;
import act.util.Lazy;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.util.S;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Lazy
public class JsonWebTokenSessionCodec implements SessionCodec {

    private JWT jwt;
    private final boolean sessionWillExpire;
    private final int ttlInMillis;
    private final String pingPath;

    @Inject
    public JsonWebTokenSessionCodec(AppConfig conf, JWT jwt) {
        ttlInMillis = conf.sessionTtl() * 1000;
        sessionWillExpire = ttlInMillis > 0;
        pingPath = conf.pingPath();
        this.jwt = $.requireNotNull(jwt);
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
            session.put(KEY_EXPIRATION, $.ms() + ttlInMillis);
        }
        return populateToken(jwt.newToken(), session).toString(jwt);
    }

    @Override
    public String encodeSession(H.Session session, int ttlInMillis) {
        if (null == session || session.isEmpty()) {
            return null;
        }
        session.id(); // ensure session ID is generated
        session.put(KEY_EXPIRATION, $.ms() + ttlInMillis);
        return populateToken(jwt.newToken(), session).toString(jwt);
    }

    @Override
    public String encodeFlash(H.Flash flash) {
        if (null == flash || flash.isEmpty()) {
            return null;
        }
        return populateToken(jwt.newToken(), flash).toString(jwt);
    }

    @Override
    public H.Session decodeSession(String encodedSession, H.Request request) {
        H.Session session = new H.Session();
        boolean newSession = true;
        if (S.notBlank(encodedSession)) {
            resolveFromJwtToken(session, encodedSession, true);
            newSession = false;
        }
        session = DefaultSessionCodec.processExpiration(
                session, $.ms(), newSession,
                sessionWillExpire, ttlInMillis, pingPath,
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

    private JWT.Token populateToken(JWT.Token token, H.KV<?> state) {
        for (Map.Entry<String, String> entry : state.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            if (H.Session.KEY_EXPIRATION.equals(k)) {
                long l = Long.parseLong(v);
                token.payload(EXPIRES_AT, l / 1000);
            } else if (H.Session.KEY_ID.equals(k)) {
                token.payload(JWT_ID, v);
            } else {
                token.payload(k, v);
            }
        }
        return token;
    }

    private void resolveFromJwtToken(H.KV<?> state, String tokenString, boolean isSession) {
        JWT.Token token = jwt.deserialize(tokenString);
        if (null == token) {
            return;
        }
        for (Map.Entry<String, Object> entry : token.payloads().entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            if (isSession && JWT.ID.equals(key)) {
                state.put(H.Session.KEY_ID, val);
            } else if (ISSUER.key().equals(key)) {
                // ignore
            } else if (EXPIRES_AT.key().equals(key)) {
                Number number = (Number) val;
                long exp = number.longValue() * 1000;
                state.put(H.Session.KEY_EXPIRATION, exp);
            } else {
                state.put(key, val);
            }
        }
    }


}
