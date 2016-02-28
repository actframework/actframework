package act.conf;

import org.osgl.util.S;
import org.osgl.util.ValueObject;

public class ConfigItem {

    private String key;
    private ValueObject val;

    public ConfigItem(String key, Config config) {
        this.key = key;
        this.val = ValueObject.of(config.get(key));
    }

    public String getKey() {
        return key;
    }

    public String getVal() {
        return S.string(val);
    }

}
