package act.session;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2020 ActFramework
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

import act.inject.DefaultValue;
import org.osgl.http.H;
import org.osgl.mvc.annotation.GetAction;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ShortLifeTokenGenerator {

    @Inject
    SessionManager sm;

    /**
     * Generate a short life session token.
     *
     * Refer: https://github.com/actframework/actframework/issues/1293
     *
     * @param ttlInSeconds token ttl in seconds, default value: 60
     * @param session injected session object of current request context
     * @return the session token with specified expiry time.
     */
    @GetAction("~session-token~")
    public String getToken(@DefaultValue("60") int ttlInSeconds, H.Session session) {
        return sm.generateSessionToken(session, ttlInSeconds);
    }

}
