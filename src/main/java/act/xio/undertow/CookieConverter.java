package act.xio.undertow;

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

import io.undertow.server.handlers.Cookie;
import org.osgl.http.H;

import java.util.Date;

public enum CookieConverter {
    ;

    public static H.Cookie undertow2osgl(Cookie uc) {
        H.Cookie c = new H.Cookie(uc.getName(), uc.getValue());
        c.domain(uc.getDomain()).httpOnly(uc.isHttpOnly())
                .path(uc.getPath()).secure(uc.isSecure())
                .version(uc.getVersion()).comment(uc.getComment());
        Integer maxAge = uc.getMaxAge();
        if (null != maxAge) {
            c.maxAge(maxAge);
        }
        Date exp = uc.getExpires();
        if (null != exp) {
            c.expires(exp);
        }
        return c;
    }

    public static Cookie osgl2undertow(H.Cookie hc) {
        return new UndertowCookieAdaptor(hc);
    }
}
