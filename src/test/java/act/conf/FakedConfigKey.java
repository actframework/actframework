package act.conf;

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

import act.Act;

import java.util.List;
import java.util.Map;

public enum FakedConfigKey implements ConfigKey {
    GATEWAY_ENABLED("gateway.enabled"),
    GATEWAY_DISABLED("gateway.disabled"),
    CONN_CNT("connection.count"),
    CONN_LONG("connection.long"),
    TIMEOUT("timeout.long"),
    DAYS("days.int"),
    HOME_TMP("tmp.dir"),
    AMOUNT("amount.float"),
    SOURCE_VERSION("source.version"),
    PATH("path"),
    FOO("foo.bar") {
        @Override
        public <T> T val(Map<String, ?> configuration) {
            Object v = configuration.get(key());
            if (null == v) {
                v = "foobar";
            } else {
                String s = v.toString();
                if ("foo".equalsIgnoreCase(s)) {
                    v = "bar";
                } else if ("bar".equalsIgnoreCase(s)) {
                    v = "foo";
                } else {
                    v = "barfoo";
                }
            }
            return (T)v;
        }
    }
    ;
    private static ConfigKeyHelper helper = new ConfigKeyHelper(Act.F.MODE_ACCESSOR, FakedConfigKey.class.getClassLoader());

    private String key;
    private Object defVal;

    FakedConfigKey(String key) {
        this(key, null);
    }

    FakedConfigKey(String key, Object defVal) {
        this.key = key;
        this.defVal = defVal;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public Object defVal() {
        return defVal;
    }

    @Override
    public <T> T val(Map<String, ?> configuration) {
        return helper.getConfiguration(this, configuration);
    }

    @Override
    public <T> List<T> implList(String key, Map<String, ?> configuration, Class<T> c) {
        return helper.getImplList(key, configuration, c);
    }
}
