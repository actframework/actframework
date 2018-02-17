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
import org.osgl.http.H;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SessionManager {

    private SessionCodec codec;

    private SessionMapper mapper;

    @Inject
    public SessionManager(AppConfig config) {
        codec = config.sessionCodec();
        mapper = config.sessionMapper();
    }

    public H.Session resolveSession(H.Request request, ActionContext context) {
        String encodedSession = mapper.readSession(request);
        context.encodedSessionToken = encodedSession;
        return null == encodedSession ? new H.Session() : codec.decodeSession(encodedSession, request);
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
        mapper.write(encodedSession, encodedFlash, response);
    }

}
