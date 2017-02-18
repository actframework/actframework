package act.security;

import act.Act;
import act.app.ActionContext;
import act.app.App;
import act.app.util.AppCrypto;
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
                StringBuilder sb = S.builder(id);
                if (S.notBlank(username)) {
                    sb.append(username);
                }
                String payload = sb.toString();
                String sign = app.sign(payload);
                return S.builder(payload).append("-").append(sign).toString();
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
