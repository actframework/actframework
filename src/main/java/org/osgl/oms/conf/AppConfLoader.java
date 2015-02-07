package org.osgl.oms.conf;

import java.util.Map;

public class AppConfLoader extends ConfLoader<AppConfig> {
    @Override
    protected AppConfig create(Map<String, ?> rawConf) {
        return new AppConfig(rawConf);
    }

    @Override
    protected String confFileName() {
        return AppConfig.CONF_FILE_NAME;
    }
}
