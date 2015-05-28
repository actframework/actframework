package act.app;

import act.conf.AppConfig;
import org.osgl._;
import org.osgl.util.C;

import java.util.Map;
import java.util.Set;

/**
 * Base class for app developer implement source code based configuration
 */
public class AppConfigurator<T extends AppConfigurator> extends AppConfig<T> {
    private Map<String, Object> userProps = C.newMap();

    protected T prop(String key, Object val) {
        userProps.put(key, val);
        return me();
    }

    public Set<String> propKeys() {
        return userProps.keySet();
    }

    public <V> V propVal(String key) {
        return _.cast(userProps.get(key));
    }

}
