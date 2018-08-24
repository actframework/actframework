package act.test.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2018 ActFramework
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

import act.util.SingletonBase;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;

@Singleton
public class CookieStore extends SingletonBase implements CookieJar {

    private Map<String, Map<String, Cookie>> store = new HashMap<>();

    @Override
    public synchronized void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        String key = key(url);
        Map<String, Cookie> cookieMap = store.get(key);
        if (null == cookieMap) {
            cookieMap = new HashMap<>();
            store.put(key, cookieMap);
        }
        for (Cookie cookie : cookies) {
            cookieMap.put(cookie.name(), cookie);
        }
    }

    @Override
    public synchronized List<Cookie> loadForRequest(HttpUrl url) {
        Map<String, Cookie> cookieMap = store.get(key(url));
        if (null == cookieMap) {
            return C.list();
        }
        return C.list(cookieMap.values());
    }

    public synchronized void clear() {
        store.clear();
    }

    private static String key(HttpUrl url) {
        String host = url.host();
        int port = url.port();
        return S.pathConcat(host, ':', S.string(port));
    }
}
