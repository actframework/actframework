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

import act.ws.DefaultSecureTicketCodec;
import act.ws.StringSecureTicketCodec;
import org.osgl.http.H.Session;

import static org.osgl.http.H.Session.KEY_EXPIRATION;
import static org.osgl.http.H.Session.KEY_ID;

public class DefaultSecureTicketCodecTest extends StringSecureTicketCodecTest {

    protected StringSecureTicketCodec codec() {
        return new DefaultSecureTicketCodec(crypto());
    }

    @Override
    public void verifySession(Session original, Session decoded) {
        compensate(KEY_EXPIRATION, original, decoded);
        compensate(KEY_ID, original, decoded);
        super.verifySession(original, decoded);
    }

    private void compensate(String key, Session original, Session decoded) {
        String val = original.get(key);
        if (null != val) {
            decoded.put(key, val);
        }
    }
}
