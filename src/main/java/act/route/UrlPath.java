package act.route;

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

import org.osgl.util.S;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulate the logic to compare an incoming URL path with a route entry path.
 * <p>
 *     Note we can't do simple String match as route entry path might contains the dynamic part
 * </p>
 */
public class UrlPath {

    static final String DYNA_PART = "";

    private final List<String> parts = new ArrayList<>();

    private UrlPath(CharSequence path) {
        String s = path.toString();
        String[] sa = s.split("/");
        for (String item: sa) {
            if (S.notBlank(item)) {
                if (item.startsWith("{") || item.contains(":")) {
                    item = DYNA_PART;
                }
                parts.add(item);
            }
        }
    }

    public int size() {
        return parts.size();
    }

    public String part(int index) {
        return parts.get(index);
    }

    public String lastPart() {
        return part(size() - 1);
    }

    /**
     * Check if the URL path is a built-in service url.
     *
     * A built in service URL starts with `~`
     * @return `true` if the URL represented by this `UrlPath` is a built-in service URL
     */
    public boolean isBuiltIn() {
        return !parts.isEmpty() && "~".equals(parts.get(0));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof UrlPath) {
            UrlPath that = (UrlPath) obj;
            if (parts.size() != that.parts.size()) {
                return false;
            }
            for (int i = parts.size() - 1; i >= 0; --i) {
                if (!matches(parts.get(i), that.parts.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static Map<String, UrlPath> paths = new HashMap<>();

    public static UrlPath of(CharSequence path) {
        UrlPath urlPath = paths.get(path.toString());
        if (null == urlPath) {
            urlPath = new UrlPath(path);
            paths.put(path.toString(), urlPath);
        }
        return urlPath;
    }

    private static boolean matches(CharSequence cs1, CharSequence cs2) {
        if (DYNA_PART.equals(cs1)) {
            return true;
        }
        int len = cs1.length();
        if (len != cs2.length()) {
            return false;
        }
        for (int i = len - 1; i >= 0; --i) {
            if (cs1.charAt(i) != cs2.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
