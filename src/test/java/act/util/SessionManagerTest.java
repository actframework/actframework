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

import act.ActTestBase;
import org.junit.Ignore;

/**
 * Test {@link SessionMapper}
 */
@Ignore
public class SessionManagerTest extends ActTestBase {
//    App app;
//    AppConfig config;
//    AppCrypto crypto;
//    OldSessionManager.CookieResolver resolver;
//    H.Session session;
//    H.Flash flash;
//
//    @Before
//    public void prepare() {
//        config = mock(AppConfig.class);
//        when(config.secret()).thenReturn("secret");
//        crypto = new AppCrypto(config);
//        app = mock(App.class);
//        when(app.config()).thenReturn(config);
//        when(app.crypto()).thenReturn(crypto);
//        when(app.sign(anyString())).thenCallRealMethod();
//        when(app.encrypt(anyString())).thenCallRealMethod();
//        when(app.decrypt(anyString())).thenCallRealMethod();
//        resolver = new OldSessionManager.CookieResolver(app);
//        session = new H.Session();
//        session.put("foo", "bar");
//        flash = new H.Flash();
//        flash.put("foo", "bar");
//    }
//
//    @Test
//    public void testSignSessionWithOnePair() {
//        String content = resolver.dissolveIntoCookieContent(session, true);
//        H.Session session1 = new H.Session();
//        resolver.resolveFromCookieContent(session1, content, true);
//        eq("bar", session1.get("foo"));
//    }
//
//    @Test
//    public void testSignSessionWithMultiplePairs() {
//        session.put("hello", "world");
//        String content = resolver.dissolveIntoCookieContent(session, true);
//        H.Session session1 = new H.Session();
//        resolver.resolveFromCookieContent(session1, content, true);
//        eq("bar", session1.get("foo"));
//        eq("world", session1.get("hello"));
//    }
//
//    @Test
//    public void testCryptoSession() {
//        when(config.encryptSession()).thenReturn(true);
//        resolver = new OldSessionManager.CookieResolver(app);
//        String content = resolver.dissolveIntoCookieContent(session, true);
//        H.Session session1 = new H.Session();
//        resolver.resolveFromCookieContent(session1, content, true);
//        eq("bar", session1.get("foo"));
//    }

}
