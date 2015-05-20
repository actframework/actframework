package org.osgl.oms.util;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgl.http.H;
import org.osgl.oms.TestBase;
import org.osgl.oms.app.App;
import org.osgl.oms.conf.AppConfig;
import org.osgl.oms.util.SessionManager;

import static org.mockito.Mockito.*;

/**
 * Test {@link org.osgl.oms.util.SessionManager}
 */
public class SessionManagerTest extends TestBase {
    App app;
    AppConfig config;
    SessionManager.CookieResolver resolver;
    H.Session session;
    H.Flash flash;

    @Before
    public void prepare() {
        config = mock(AppConfig.class);
        when(config.secret()).thenReturn("secret");
        app = mock(App.class);
        when(app.config()).thenReturn(config);
        when(app.sign(anyString())).thenCallRealMethod();
        when(app.encrypt(anyString())).thenCallRealMethod();
        when(app.decrypt(anyString())).thenCallRealMethod();
        resolver = new SessionManager.CookieResolver(app);
        session = new H.Session();
        session.put("foo", "bar");
        flash = new H.Flash();
        flash.put("foo", "bar");
    }

    @Test
    public void testSignSessionWithOnePair() {
        String content = resolver.dissolveIntoCookieContent(session, true);
        H.Session session1 = new H.Session();
        resolver.resolveFromCookieContent(session1, content, true);
        eq("bar", session1.get("foo"));
    }

    @Test
    public void testSignSessionWithMultiplePairs() {
        session.put("hello", "world");
        String content = resolver.dissolveIntoCookieContent(session, true);
        H.Session session1 = new H.Session();
        resolver.resolveFromCookieContent(session1, content, true);
        eq("bar", session1.get("foo"));
        eq("world", session1.get("hello"));
    }

    @Test
    public void testCryptoSession() {
        when(config.encryptSession()).thenReturn(true);
        resolver = new SessionManager.CookieResolver(app);
        String content = resolver.dissolveIntoCookieContent(session, true);
        H.Session session1 = new H.Session();
        resolver.resolveFromCookieContent(session1, content, true);
        eq("bar", session1.get("foo"));
    }

}
