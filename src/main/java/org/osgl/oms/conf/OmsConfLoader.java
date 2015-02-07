package org.osgl.oms.conf;

import java.util.Map;

public class OmsConfLoader extends ConfLoader<OmsConfig> {
    @Override
    protected OmsConfig create(Map<String, ?> rawConf) {
        return new OmsConfig(rawConf);
    }

    @Override
    protected String confFileName() {
        return OmsConfig.CONF_FILE_NAME;
    }
}
