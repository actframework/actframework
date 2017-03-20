package act.util;

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
import act.conf.AppConfig;
import act.plugin.Plugin;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.http.H.Session;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.*;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static act.Destroyable.Util.tryDestroyAll;
import static org.osgl.http.H.Session.KEY_EXPIRATION;
import static org.osgl.http.H.Session.KEY_EXPIRE_INDICATOR;

/**
 * Resolve/Persist session/flash
 */
public class SessionManager extends DestroyableBase {

    private static Logger logger = L.get(SessionManager.class);

    private C.List<Listener> registry = C.newList();
    private Map<App, CookieResolver> resolvers = C.newMap();
    private CookieResolver theResolver = null;

    public SessionManager() {
    }

    @Override
    protected void releaseResources() {
        tryDestroyAll(registry, ApplicationScoped.class);
        registry = null;

        tryDestroyAll(resolvers.values(), ApplicationScoped.class);
        resolvers = null;

        theResolver = null;
    }

    public void register(Listener listener) {
        if (!registry.contains(listener)) registry.add(listener);
    }

    public <T extends Listener> T findListener(Class<T> clz) {
        for (Listener l: registry) {
            if (clz.isAssignableFrom(l.getClass())) {
                return (T)l;
            }
        }
        return null;
    }

    public Session resolveSession(ActionContext context) {
        Session session = getResolver(context).resolveSession(context);
        return session;
    }

    public void fireSessionResolved(ActionContext ctx) {
        sessionResolved(ctx.session(), ctx);
    }

    public H.Flash resolveFlash(ActionContext context) {
        return getResolver(context).resolveFlash(context);
    }

    public H.Cookie dissolveSession(ActionContext context) {
        onSessionDissolve();
        return getResolver(context).dissolveSession(context);
    }

    public H.Cookie dissolveFlash(ActionContext context) {
        return getResolver(context).dissolveFlash(context);
    }

    private void sessionResolved(Session session, ActionContext context) {
        for (Listener l : registry) {
            l.sessionResolved(session, context);
        }
    }

    private void onSessionDissolve() {
        for (Listener l : registry) {
            l.onSessionDissolve();
        }
    }

    private CookieResolver getResolver(ActionContext context) {
        App app = context.app();
        if (Act.multiTenant()) {
            CookieResolver resolver = resolvers.get(app);
            if (null == resolver) {
                resolver = new CookieResolver(app);
                resolvers.put(app, resolver);
            }
            return resolver;
        } else {
            if (theResolver == null) {
                theResolver = new CookieResolver(app);
            }
            return theResolver;
        }
    }

    public static abstract class Listener extends DestroyableBase implements Plugin {
        @Override
        public void register() {
            Act.sessionManager().register(this);
        }

        /**
         * Called once a session object has been resolved from session
         * cookie of the incoming request
         * <p>
         * Plugin use this hook to implement specific logic, e.g.
         * grab username from session to initialize authentication
         * </p>
         *
         * @param session the session object resolved from cookie
         */
        public void sessionResolved(Session session, ActionContext context) {}

        /**
         * Called before a session is about to be written to a cookie
         * <p>Plugin use this hook to release resources associated with the
         * computational context</p>
         */
        public void onSessionDissolve() {}
    }

    static class CookieResolver {

        private App app;
        private AppConfig conf;
        private boolean encryptSession;
        private boolean persistentSession;
        private boolean sessionSecure;
        private long ttl;
        private boolean sessionWillExpire;
        private SessionMapper sessionMapper;
        private String sessionCookieName;
        private String flashCookieName;

        CookieResolver(App app) {
            E.NPE(app);
            this.app = app;
            this.conf = app.config();

            this.encryptSession = conf.encryptSession();
            this.persistentSession = conf.persistSession();
            this.sessionSecure = conf.sessionSecure();
            long ttl = conf.sessionTtl();
            this.ttl = ttl * 1000L;
            sessionWillExpire = ttl > 0;
            sessionMapper = conf.sessionMapper();
            sessionCookieName = conf.sessionCookieName();
            flashCookieName = conf.flashCookieName();
        }

        Session resolveSession(ActionContext context) {
            H.Request req = context.req();
            context.preCheckCsrf();
            String val = sessionMapper.deserializeSession(context);

            Session session = new Session();
            long now = $.ms();
            if (S.blank(val)) {
                session = processExpiration(session, now, true, req);
            } else {
                resolveFromCookieContent(session, val, true);
                session = processExpiration(session, now, false, req);
            }
            context.checkCsrf(session);
            return session;
        }

        H.Flash resolveFlash(ActionContext context) {
            H.Flash flash = new H.Flash();
            String val = sessionMapper.deserializeFlash(context);
            if (null != val) {
                resolveFromCookieContent(flash, val, false);
                flash.discard(); // prevent cookie content from been output to response again
            }
            return flash;
        }

        H.Cookie dissolveSession(ActionContext context) {
            context.setCsrfCookieAndRenderArgs();
            Session session = context.session();
            if (null == session) {
                return null;
            }
            boolean sessionChanged = session.changed();
            if (!sessionChanged && (session.empty() || !sessionWillExpire)) {
                // Nothing changed and no cookie-expire or empty, consequently send nothing back.
                return null;
            }
            H.Cookie cookie;
            if (session.empty()) {
                // session is empty, delete it from cookie
                cookie = createCookie(sessionCookieName, "");
            } else {
                session.id(); // ensure session ID is generated
                if (sessionWillExpire && !session.contains(KEY_EXPIRATION)) {
                    // session get cleared before
                    session.put(KEY_EXPIRATION, $.ms() + ttl);
                }
                String data = dissolveIntoCookieContent(session, true);
                cookie = createCookie(sessionCookieName, data);
            }
            // I can't clear here b/c template might use it:: session.clear();
            return cookie;
        }

        H.Cookie dissolveFlash(ActionContext context) {
            H.Flash flash = context.flash();
            if (null == flash || flash.isEmpty()) {
                return null;
            }
            String data = dissolveIntoCookieContent(flash.out(), false);
            H.Cookie cookie = createCookie(flashCookieName, data);
            // I can't clear here b/c template might use it:: flash.clear();
            return cookie;
        }

        void resolveFromCookieContent(H.KV<?> kv, String content, boolean isSession) {
            String data = Codec.decodeUrl(content, Charsets.UTF_8);
            if (isSession) {
                if (encryptSession) {
                    try {
                        data = app.decrypt(data);
                    } catch (Exception e) {
                        return;
                    }
                }
                int firstDashIndex = data.indexOf("-");
                if (firstDashIndex < 0) {
                    return;
                }
                String sign = data.substring(0, firstDashIndex);
                data = data.substring(firstDashIndex + 1);
                String sign1 = app.sign(data);
                if (!sign.equals(sign1)) {
                    return;
                }
            }
            List<char[]> pairs = split(data.toCharArray(), '\u0000');
            if (pairs.isEmpty()) return;
            for (char[] pair: pairs) {
                List<char[]> kAndV = split(pair, '\u0001');
                int sz = kAndV.size();
                if (sz != 2) {
                    S.Buffer sb = S.newBuffer();
                    for (int i = 0; i < sz; ++i) {
                        if (i > 0) sb.append(":");
                        sb.append(Arrays.toString(kAndV.get(i)));
                    }
                    logger.warn("unexpected KV string: %S", sb.toString());
                } else {
                    kv.put(new String(kAndV.get(0)), new String(kAndV.get(1)));
                }
            }
        }

        private List<char[]> split(char[] content, char separator) {
            int len = content.length;
            if (0 == len) {
                return C.list();
            }
            List<char[]> l = new ArrayList<char[]>();
            int start = 0;
            for (int i = 0; i < len; ++i) {
                char c = content[i];
                if (c == separator) {
                    if (i == start) {
                        start++;
                        continue;
                    }
                    char[] ca = new char[i - start];
                    System.arraycopy(content, start, ca, 0, i - start);
                    l.add(ca);
                    start = i + 1;
                }
            }
            if (start == 0) {
                l.add(content);
            } else {
                char[] ca = new char[len - start];
                System.arraycopy(content, start, ca, 0, len - start);
                l.add(ca);
            }
            return l;
        }

        String dissolveIntoCookieContent(H.KV<?> kv, boolean isSession) {
            S.Buffer sb = S.buffer();
            int i = 0;
            for (String k : kv.keySet()) {
                if (i > 0) {
                    sb.append("\u0000");
                }
                sb.append(k);
                sb.append("\u0001");
                sb.append(kv.get(k));
                i++;
            }
            String data = sb.toString();
            if (isSession) {
                String sign = app.sign(data);
                data = S.concat(sign, "-", data);
                if (encryptSession) {
                    data = app.encrypt(data);
                }
            }
            data = Codec.encodeUrl(data, Charsets.UTF_8);
            return data;
        }

        private Session processExpiration(Session session, long now, boolean freshSession, H.Request request) {
            if (!sessionWillExpire) return session;
            long expiration = now + ttl;
            if (freshSession) {
                // no previous cookie to restore; but we need to set the timestamp in the new cookie
                // note we use `load` API instead of `put` because we don't want to set the dirty flag
                // in this case
                session.load(KEY_EXPIRATION, String.valueOf(expiration));
            } else {
                String s = session.get(KEY_EXPIRATION);
                long oldTimestamp = null == s ? -1 : Long.parseLong(s);
                long newTimestamp = expiration;
                // Verify that the session contains a timestamp, and that it's not expired
                if (oldTimestamp < 0) {
                    // invalid session, reset it
                    session = new Session();
                } else {
                    if (oldTimestamp < now) {
                        // Session expired
                        session = new Session();
                        session.put(KEY_EXPIRE_INDICATOR, true);
                    } else {
                        session.remove(KEY_EXPIRE_INDICATOR);
                        boolean skipUpdateExpiration = S.eq(conf.pingPath(), request.url());
                        if (skipUpdateExpiration) {
                            newTimestamp = oldTimestamp;
                        }
                    }
                }
                session.put(KEY_EXPIRATION, newTimestamp);
            }
            return session;
        }

        private H.Cookie createCookie(String name, String value) {
            H.Cookie cookie = new H.Cookie(name, value);
            cookie.path("/");
            cookie.domain(conf.cookieDomain());
            cookie.httpOnly(true);
            cookie.secure(sessionSecure);
            if (sessionWillExpire && persistentSession) {
                cookie.maxAge((int) (ttl / 1000));
            }
            return cookie;
        }
    }
}
