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
import org.osgl.util.E;

import java.util.Date;

public class UndertowCookieAdaptor implements Cookie {
    H.Cookie hc;

    public UndertowCookieAdaptor(H.Cookie cookie) {
        E.NPE(cookie);
        hc = cookie;
    }

    @Override
    public String getName() {
        return hc.name();
    }

    @Override
    public String getValue() {
        return hc.value();
    }

    @Override
    public Cookie setValue(String value) {
        hc.value(value);
        return this;
    }

    @Override
    public String getPath() {
        return hc.path();
    }

    @Override
    public Cookie setPath(String path) {
        hc.path(path);
        return this;
    }

    @Override
    public String getDomain() {
        return hc.domain();
    }

    @Override
    public Cookie setDomain(String domain) {
        hc.domain(domain);
        return this;
    }

    @Override
    public Integer getMaxAge() {
        return hc.maxAge();
    }

    @Override
    public Cookie setMaxAge(Integer maxAge) {
        hc.maxAge(maxAge);
        return this;
    }

    @Override
    public boolean isDiscard() {
        return hc.maxAge() < 0;
    }

    @Override
    public Cookie setDiscard(boolean discard) {
        if (discard) {
            hc.maxAge(-1);
        }
        return this;
    }

    @Override
    public boolean isSecure() {
        return hc.secure();
    }

    @Override
    public Cookie setSecure(boolean secure) {
        hc.secure(secure);
        return this;
    }

    @Override
    public int getVersion() {
        return hc.version();
    }

    @Override
    public Cookie setVersion(int version) {
        hc.version(version);
        return this;
    }

    @Override
    public boolean isHttpOnly() {
        return hc.httpOnly();
    }

    @Override
    public Cookie setHttpOnly(boolean httpOnly) {
        hc.httpOnly(httpOnly);
        return this;
    }

    @Override
    public Date getExpires() {
        return hc.expires();
    }

    @Override
    public Cookie setExpires(Date expires) {
        hc.expires(expires);
        return this;
    }

    @Override
    public String getComment() {
        return hc.comment();
    }

    @Override
    public Cookie setComment(String comment) {
        hc.comment(comment);
        return this;
    }
}
