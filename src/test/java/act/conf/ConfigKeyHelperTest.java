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
import act.ActTestBase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.Map;

public class ConfigKeyHelperTest extends ActTestBase {

    ConfigKeyHelper helper = new ConfigKeyHelper(new $.F0<Act.Mode>() {
        @Override
        public Act.Mode apply() throws NotAppliedException, $.Break {
            return Act.Mode.DEV;
        }
    }, ConfigKeyHelperTest.class.getClassLoader());

    private C.Map<String, Object> conf;

    @Before
    public void prepare() {
        conf = C.newMap((Map)System.getProperties());
        conf.putAll(System.getenv());
    }

    @Test
    public void fetchBooleanByEnabled() {
        put(FakedConfigKey.GATEWAY_ENABLED, "true");
        eq(true, helper.getConfiguration(FakedConfigKey.GATEWAY_ENABLED, conf));
    }

    @Test
    public void fetchBooleanByDisabled() {
        put(FakedConfigKey.GATEWAY_DISABLED, "false");
        eq(true, helper.getConfiguration(FakedConfigKey.GATEWAY_ENABLED, conf));
    }

    @Test
    public void fetchBooleanByEnabledWithoutSuffix() {
        conf.put(S.beforeLast(FakedConfigKey.GATEWAY_ENABLED.key(), "."), "true");
        eq(true, helper.getConfiguration(FakedConfigKey.GATEWAY_ENABLED, conf));
        eq(false, helper.getConfiguration(FakedConfigKey.GATEWAY_DISABLED, conf));
    }

    @Test
    public void fetchBooleanByEnabledWithBooleanTypePut() {
        put(FakedConfigKey.GATEWAY_ENABLED, true);
        eq(true, helper.getConfiguration(FakedConfigKey.GATEWAY_ENABLED, conf));
    }

    @Test
    public void fetchInt() {
        put(FakedConfigKey.CONN_CNT, "10");
        eq(10, helper.getConfiguration(FakedConfigKey.CONN_CNT, conf));
        put(FakedConfigKey.CONN_CNT, 30);
        eq(30, helper.getConfiguration(FakedConfigKey.CONN_CNT, conf));
    }

    @Test
    public void fetchIntWithModeConf() {
        put(FakedConfigKey.CONN_CNT, "10");
        eq(10, helper.getConfiguration(FakedConfigKey.CONN_CNT, conf));
    }

    @Test
    public void fetchLong() {
        put(FakedConfigKey.CONN_LONG, "100");
        eq(100l, helper.getConfiguration(FakedConfigKey.CONN_LONG, conf));
        conf.put(FakedConfigKey.CONN_LONG.key(), Long.MAX_VALUE);
        eq(Long.MAX_VALUE, helper.getConfiguration(FakedConfigKey.CONN_LONG, conf));
    }

    @Test
    public void fetchFromSysProps() {
        put(FakedConfigKey.SOURCE_VERSION, "${java.version}");
        eq(System.getProperty("java.version"), helper.getConfiguration(FakedConfigKey.SOURCE_VERSION, conf));
    }

    @Test
    public void fetchFromSysEnv() {
        put(FakedConfigKey.PATH, "${PATH}");
        eq(System.getenv("PATH"), helper.getConfiguration(FakedConfigKey.PATH, conf));
    }

    @Test
    public void testVariableEvaluation() {
        put("foo.bar", "123");
        put("xyz", "aaa");
        put("p", "abc${foo.bar}/xyz/${xyz}/ddd");
        eq("abc123/xyz/aaa/ddd", helper.getConfiguration("p", null, conf));
    }

    @Test
    @Ignore
    public void fetchImpl() {
        E.tbd("fetchImpl");
    }

    @Test
    @Ignore
    public void fetchDir() {
        E.tbd();
    }

    private void put(ConfigKey key, Object v) {
        conf.put(key.key(), v);
    }

    private void put(String key, Object v) {
        conf.put(key, v);
    }
}
