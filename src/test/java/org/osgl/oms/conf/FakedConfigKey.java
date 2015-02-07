package org.osgl.oms.conf;

import org.osgl.oms.OMS;

import java.util.List;
import java.util.Map;

public enum FakedConfigKey implements ConfigKey {
    GATEWAY_ENABLED("gateway.enabled"),
    GATEWAY_DISABLED("gateway.disabled"),
    CONN_CNT("connection.count"),
    CONN_TTL("connection.ttl"),
    TIMEOUT("timeout.long"),
    DAYS("days.int"),
    HOME_TMP("tmp.dir"),
    AMOUNT("amount.float"),
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
    private static ConfigKeyHelper helper = new ConfigKeyHelper(OMS.F.MODE_ACCESSOR);

    private String key;
    private Object defVal;

    private FakedConfigKey(String key) {
        this(key, null);
    }

    private FakedConfigKey(String key, Object defVal) {
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
