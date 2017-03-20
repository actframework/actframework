package act.inject.param;

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

import org.osgl.util.E;
import org.osgl.util.S;

public enum HttpRequestParamEncode {
    /**
     * Example: `foo[bar][id]`
     */
    JQUERY() {
        @Override
        protected String _concat(String[] path, int len) {
            S.Buffer buf = S.buffer(path[0]);
            for (int i = 1; i < len; ++i) {
                buf.append("[").append(path[i]).append("]");
            }
            return buf.toString();
        }
    },

    /**
     * Example: `foo.bar.id`
     */
    DOT_NOTATION() {
        @Override
        protected String _concat(String[] path, int len) {
            S.Buffer buf = S.buffer(path[0]);
            for (int i = 1; i < len; ++i) {
                buf.append(".").append(path[i]);
            }
            return buf.toString();
        }
    };

    public final String concat(ParamKey key) {
        return concat(key.seq());
    }

    public final String concat(String[] path) {
        int len = path.length;
        E.illegalArgumentIf(len < 1);
        if (len == 1) {
            return path[0];
        }
        return _concat(path, len);
    }

    protected abstract String _concat(String[] path, int len);

    public static HttpRequestParamEncode next(HttpRequestParamEncode encode) {
        HttpRequestParamEncode[] all = values();
        int id = encode.ordinal();
        if (id < all.length - 1) {
            return all[id + 1];
        } else {
            return all[0];
        }
    }
}
