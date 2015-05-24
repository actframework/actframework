package act.conf;

import java.util.Map;

public class ActConfLoader extends ConfLoader<ActConfig> {
    @Override
    protected ActConfig create(Map<String, ?> rawConf) {
        return new ActConfig(rawConf);
    }

    @Override
    protected String confFileName() {
        return ActConfig.CONF_FILE_NAME;
    }
}
