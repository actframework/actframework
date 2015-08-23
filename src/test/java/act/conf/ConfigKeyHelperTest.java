package act.conf;

import act.Act;
import act.TestBase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgl._;
import org.osgl.exception.NotAppliedException;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

public class ConfigKeyHelperTest extends TestBase {

    ConfigKeyHelper helper = new ConfigKeyHelper(new _.F0<Act.Mode>() {
        @Override
        public Act.Mode apply() throws NotAppliedException, _.Break {
            return Act.Mode.DEV;
        }
    });

    private C.Map<String, Object> conf;

    @Before
    public void prepare() {
        conf = C.newMap();
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
        put(FakedConfigKey.CONN_TTL, "100");
        eq(100l, helper.getConfiguration(FakedConfigKey.CONN_TTL, conf));
        conf.put(FakedConfigKey.CONN_TTL.key(), Long.MAX_VALUE);
        eq(Long.MAX_VALUE, helper.getConfiguration(FakedConfigKey.CONN_TTL, conf));
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
