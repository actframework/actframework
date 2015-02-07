package org.osgl.oms.conf;

import org.junit.Before;
import org.junit.Test;
import org.osgl._;
import org.osgl.exception.NotAppliedException;
import org.osgl.oms.OMS;
import org.osgl.oms.TestBase;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import static org.osgl.oms.OMS.Mode.DEV;
import static org.osgl.oms.OMS.Mode.UAT;
import static org.osgl.oms.conf.FakedConfigKey.*;

public class ConfigKeyHelperTest extends TestBase {

    ConfigKeyHelper helper = new ConfigKeyHelper(new _.F0<OMS.Mode>() {
        @Override
        public OMS.Mode apply() throws NotAppliedException, _.Break {
            return UAT;
        }
    });

    private C.Map<String, Object> conf;

    @Before
    public void prepare() {
        conf = C.newMap();
    }

    @Test
    public void fetchBooleanByEnabled() {
        put(GATEWAY_ENABLED, "true");
        eq(true, helper.getConfiguration(GATEWAY_ENABLED, conf));
    }

    @Test
    public void fetchBooleanByDisabled() {
        put(GATEWAY_DISABLED, "false");
        eq(true, helper.getConfiguration(GATEWAY_ENABLED, conf));
    }

    @Test
    public void fetchBooleanByEnabledWithoutSuffix() {
        conf.put(S.beforeLast(GATEWAY_ENABLED.key(), "."), "true");
        eq(true, helper.getConfiguration(GATEWAY_ENABLED, conf));
        eq(false, helper.getConfiguration(GATEWAY_DISABLED, conf));
    }

    @Test
    public void fetchBooleanByEnabledWithBooleanTypePut() {
        put(GATEWAY_ENABLED, true);
        eq(true, helper.getConfiguration(GATEWAY_ENABLED, conf));
    }

    @Test
    public void fetchInt() {
        put(CONN_CNT, "10");
        eq(10, helper.getConfiguration(CONN_CNT, conf));
        put(CONN_CNT, 30);
        eq(30, helper.getConfiguration(CONN_CNT, conf));
    }

    @Test
    public void fetchIntWithModeConf() {
        put(CONN_CNT, "10");
        put(UAT.configKey(CONN_CNT.key()), "20");
        put(DEV.configKey(CONN_CNT.key()), 40);
        eq(20, helper.getConfiguration(CONN_CNT, conf));
    }

    @Test
    public void fetchLong() {
        put(CONN_TTL, "100");
        eq(100l, helper.getConfiguration(CONN_TTL, conf));
        conf.put(CONN_TTL.key(), Long.MAX_VALUE);
        eq(Long.MAX_VALUE, helper.getConfiguration(CONN_TTL, conf));
    }

    @Test
    public void fetchImpl() {
        E.tbd("fetchImpl");
    }

    @Test
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
