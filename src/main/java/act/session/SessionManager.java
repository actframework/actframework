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

import act.app.ActionContext;
import act.conf.AppConfig;
import act.util.LogSupportedDestroyableBase;
import org.osgl.cache.CacheService;
import org.osgl.http.H;

import javax.inject.*;

@Singleton
public class SessionManager extends LogSupportedDestroyableBase {

    private SessionCodec codec;
    private SessionMapper mapper;
    private int sessionTimeout;

    private CacheService logoutSessionCache;

    @Inject
    public SessionManager(AppConfig config, @Named("act-logout-session") CacheService cacheService) {
        codec = config.sessionCodec();
        mapper = config.sessionMapper();
        sessionTimeout = config.sessionTtl();
        logoutSessionCache = cacheService;
    }

    public void logout(H.Session session) {
        if (sessionTimeout > 0) {
            logoutSessionCache.put(session.id(), "", sessionTimeout);
        }
        session.clear();
    }

    public H.Session resolveSession(H.Request request, ActionContext context) {
        String encodedSession = mapper.readSession(request);
        context.encodedSessionToken = encodedSession;
        if (null == encodedSession) {
            return new H.Session();
        }
        H.Session session = codec.decodeSession(encodedSession, request);
        if (sessionTimeout <= 0) {
            // session never timeout
            return session;
        }
        // check if session has been logged out
        String id = session.id();
        if (null != logoutSessionCache.get(id)) {
            session = new H.Session();
        }
        return session;
    }

    public H.Flash resolveFlash(H.Request request) {
        String encodedFlash = mapper.readFlash(request);
        if (null != encodedFlash) {
            H.Flash flash = codec.decodeFlash(encodedFlash);
            flash.discard();
            return flash;
        }
        return new H.Flash();
    }

    public void dissolveState(H.Session session, H.Flash flash, H.Response response) {
        String encodedSession = codec.encodeSession(session);
        // flash could be null if precheck CSRF failed
        String encodedFlash = codec.encodeFlash(flash);
        long expiry = session.expiry();
        if (expiry > 0) {
            mapper.writeExpiration(session.expiry(), response);
        }
        mapper.write(encodedSession, encodedFlash, response);
    }

    public String generateSessionToken(H.Session session, int ttlInSeconds) {
        return codec.encodeSession(session, ttlInSeconds);
    }

}
