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

import act.conf.AppConfig;
import act.ws.StringSecureTicketCodec;
import act.ws.UsernameSecureTicketCodec;
import org.osgl.http.H;

public class UsernameSecureTicketCodecTest extends StringSecureTicketCodecTest {

    private static final String sessionKeyUsername = new AppConfig<>().sessionKeyUsername();

    protected StringSecureTicketCodec codec() {
        return new UsernameSecureTicketCodec(crypto(), sessionKeyUsername);
    }

    @Override
    protected void prepareSession(H.Session session) {
        session.put(sessionKeyUsername, "abc@123.com");
    }

    @Override
    public void verifySession(H.Session original, H.Session decoded) {
        eq(original.get(sessionKeyUsername), decoded.get(sessionKeyUsername));
    }
}
