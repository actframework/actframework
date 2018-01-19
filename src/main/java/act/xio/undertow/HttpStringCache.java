package act.xio.undertow;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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

import io.undertow.util.HttpString;
import org.osgl.http.H;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class HttpStringCache {

    static final HttpStringCache HEADER = new HttpStringCache();
    static {
        HttpStringCache cache = HEADER;
        cache.get(H.Header.Names.ACCEPT);
        cache.get(H.Header.Names.ACCESS_CONTROL_ALLOW_ORIGIN);
        cache.get(H.Header.Names.ACCESS_CONTROL_ALLOW_METHODS);
        cache.get(H.Header.Names.ACCESS_CONTROL_ALLOW_HEADERS);
        cache.get(H.Header.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS);
        cache.get(H.Header.Names.ACCESS_CONTROL_EXPOSE_HEADERS);
        cache.get(H.Header.Names.ACCESS_CONTROL_MAX_AGE);
        cache.get(H.Header.Names.ACCESS_CONTROL_REQUEST_METHOD);
        cache.get(H.Header.Names.ACCESS_CONTROL_REQUEST_HEADERS);
        cache.get(H.Header.Names.AUTHORIZATION);
        cache.get(H.Header.Names.CONTENT_DISPOSITION);
        cache.get(H.Header.Names.CONTENT_ENCODING);
        cache.get(H.Header.Names.CONTENT_LENGTH);
        cache.get(H.Header.Names.CONTENT_SECURITY_POLICY);
        cache.get(H.Header.Names.CONTENT_TYPE);
        cache.get(H.Header.Names.COOKIE);
        cache.get(H.Header.Names.DATE);
        cache.get(H.Header.Names.ETAG);
        cache.get(H.Header.Names.EXPIRES);
        cache.get(H.Header.Names.HOST);
        cache.get(H.Header.Names.HTTP_CLIENT_IP);
        cache.get(H.Header.Names.HTTP_X_FORWARDED_FOR);
        cache.get(H.Header.Names.IF_MATCH);
        cache.get(H.Header.Names.IF_MODIFIED_SINCE);
        cache.get(H.Header.Names.IF_NONE_MATCH);
        cache.get(H.Header.Names.IF_RANGE);
        cache.get(H.Header.Names.IF_UNMODIFIED_SINCE);
        cache.get(H.Header.Names.LAST_MODIFIED);
        cache.get(H.Header.Names.ORIGIN);
        cache.get(H.Header.Names.PRAGMA);
        cache.get(H.Header.Names.RANGE);
        cache.get(H.Header.Names.REFERER);
        cache.get(H.Header.Names.SEC_WEBSOCKET_KEY1);
        cache.get(H.Header.Names.SEC_WEBSOCKET_KEY2);
        cache.get(H.Header.Names.SEC_WEBSOCKET_LOCATION);
        cache.get(H.Header.Names.SEC_WEBSOCKET_ORIGIN);
        cache.get(H.Header.Names.SEC_WEBSOCKET_PROTOCOL);
        cache.get(H.Header.Names.SEC_WEBSOCKET_VERSION);
        cache.get(H.Header.Names.SEC_WEBSOCKET_KEY);
        cache.get(H.Header.Names.SEC_WEBSOCKET_ACCEPT);
        cache.get(H.Header.Names.SERVER);
        cache.get(H.Header.Names.SET_COOKIE);
        cache.get(H.Header.Names.SET_COOKIE2);
        cache.get(H.Header.Names.UPGRADE);
        cache.get(H.Header.Names.USER_AGENT);
        cache.get(H.Header.Names.WEBSOCKET_LOCATION);
        cache.get(H.Header.Names.WEBSOCKET_ORIGIN);
        cache.get(H.Header.Names.WEBSOCKET_PROTOCOL);
        cache.get(H.Header.Names.WWW_AUTHENTICATE);
        cache.get(H.Header.Names.X_REQUESTED_WITH);
        cache.get(H.Header.Names.X_FORWARDED_HOST);
        cache.get(H.Header.Names.X_FORWARDED_FOR);
        cache.get(H.Header.Names.X_FORWARDED_PROTO);
        cache.get(H.Header.Names.X_FORWARDED_SSL);
        cache.get(H.Header.Names.X_HTTP_METHOD_OVERRIDE);
        cache.get(H.Header.Names.X_URL_SCHEME);
        cache.get(H.Header.Names.X_XSRF_TOKEN);
    }

    private final ConcurrentMap<String, HttpString> lookup = new ConcurrentHashMap<>();

    HttpString get(String s) {
        HttpString hs = lookup.get(s);
        if (null == hs) {
            hs = new HttpString(s);
            lookup.putIfAbsent(s, hs);
        }
        return hs;
    }



}
