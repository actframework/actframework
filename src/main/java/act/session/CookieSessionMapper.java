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
import org.osgl.http.H;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implement {@link SessionMapper} with HTTP cookie
 */
@Singleton
public class CookieSessionMapper implements SessionMapper {

    private ExpirationMapper expirationMapper;
    private String sessionCookieName;
    private String flashCookieName;
    private String cookieDomain;
    private boolean sessionSecure;
    private boolean sessionWillExpire;
    private boolean persistentSession;
    private int ttl;

    @Inject
    public CookieSessionMapper(AppConfig conf) {
        sessionCookieName = conf.sessionCookieName();
        flashCookieName = conf.flashCookieName();
        cookieDomain = conf.cookieDomain();
        sessionSecure = conf.sessionSecure();
        persistentSession = conf.persistSession();
        ttl = conf.sessionTtl();
        sessionWillExpire = ttl > 0;
        expirationMapper = new ExpirationMapper(conf);
    }

    @Override
    public void writeExpiration(long expiration, H.Response response) {
        expirationMapper.writeExpiration(expiration, response);
    }

    @Override
    public void write(String session, String flash, H.Response response) {
        writeState(session, sessionCookieName, response);
        writeState(flash, flashCookieName, response);
    }

    @Override
    public String readSession(H.Request request) {
        return readState(sessionCookieName, request);
    }

    @Override
    public String readFlash(H.Request request) {
        return readState(flashCookieName, request);
    }

    private void writeState(String state, String cookieName, H.Response response) {
        if (null == state) {
            return;
        }
        H.Cookie cookie = createCookie(cookieName, state);
        response.addCookie(cookie);
    }

    private H.Cookie createCookie(String name, String value) {
        H.Cookie cookie = new H.Cookie(name, value);
        cookie.path("/");
        cookie.domain(cookieDomain);
        cookie.httpOnly(true);
        cookie.secure(sessionSecure);
        if (sessionWillExpire && persistentSession) {
            cookie.maxAge(ttl);
        }
        return cookie;
    }

    private String readState(String cookieName, H.Request request) {
        H.Cookie cookie = request.cookie(cookieName);
        return null == cookie ? null : cookie.value();
    }

}
