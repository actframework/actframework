package act.security;

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

import act.Act;
import act.app.ActionContext;
import act.app.App;
import act.crypto.AppCrypto;
import org.osgl.http.H;
import org.osgl.util.S;

public interface CSRFProtector {

    boolean verifyToken(String token, H.Session session, App app);

    String retrieveToken(H.Session session, String cookieName, App app);

    String generateToken(H.Session session, App app);

    void clearExistingToken(H.Session session, String cookieName);

    void outputToken(String token, String cookieName, String cookieDomain, ActionContext context);

    enum Predefined implements CSRFProtector {
        HMAC() {
            @Override
            public boolean verifyToken(String token, H.Session session, App app) {
                return S.eq(Act.app().decrypt(token), generateToken(session, app));
            }

            @Override
            public String retrieveToken(H.Session session, String cookieName, App app) {
                return null;
            }

            @Override
            public void clearExistingToken(H.Session session, String cookieName) {}

            @Override
            public String generateToken(H.Session session, App app) {
                String id = session.id();
                String username = session.get(app.config().sessionKeyUsername());
                String payload = S.concat(id, username);
                String sign = app.sign(payload);
                return S.concat(payload, "-", sign);
            }

            @Override
            public void outputToken(String token, String cookieName, String cookieDomain, ActionContext context) {
            }
        },

        RANDOM() {

            @Override
            public boolean verifyToken(String token, H.Session session, App app) {
                String tokenInSession = session.get(app.config().csrfCookieName());
                if (S.eq(token, tokenInSession)) {
                    return true;
                }
                AppCrypto crypto = Act.crypto();
                return S.eq(crypto.decrypt(token), crypto.decrypt(tokenInSession));
            }

            @Override
            public String retrieveToken(H.Session session, String cookieName, App app) {
                return session.get(cookieName);
            }

            @Override
            public void clearExistingToken(H.Session session, String cookieName) {
                session.remove(cookieName);
            }

            @Override
            public String generateToken(H.Session session, App app) {
                return String.valueOf(Act.crypto().generateRandomInt());
            }

            @Override
            public void outputToken(String token, String cookieName, String cookieDomain, ActionContext context) {
                context.session().put(cookieName, token);
            }
        };

        public static CSRFProtector valueOfIgnoreCase(String s) {
            if (S.eq(HMAC.name(), s.toUpperCase())) {
                return HMAC;
            } else if (S.eq(RANDOM.name(), s.toUpperCase())) {
                return RANDOM;
            }
            return null;
        }

    }

}
