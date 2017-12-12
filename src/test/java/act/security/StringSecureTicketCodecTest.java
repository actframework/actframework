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

import act.ActTestBase;
import act.crypto.AppCrypto;
import act.conf.AppConfig;
import act.ws.StringSecureTicketCodec;
import org.junit.Test;
import org.osgl.http.H;
import org.osgl.util.S;

public abstract class StringSecureTicketCodecTest extends ActTestBase {

    protected abstract StringSecureTicketCodec codec();

    protected AppCrypto crypto() {
        return new AppCrypto(new AppConfig());
    }

    @Test
    public void test() {
        H.Session session = new H.Session();
        String foo = S.random();
        session.put("foo", foo);
        prepareSession(session);
        StringSecureTicketCodec codec = codec();
        String ticket = codec.createTicket(session);
        H.Session session2 = codec.parseTicket(ticket);
        verifySession(session, session2);
    }

    protected void prepareSession(H.Session session) {
    }

    public void verifySession(H.Session original, H.Session decoded) {
        eq(original, decoded);
    }
}
