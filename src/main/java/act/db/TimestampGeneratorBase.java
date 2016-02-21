package act.db;

import act.Act;
import act.plugin.Plugin;

public abstract class TimestampGeneratorBase<TIMESTAMP_TYPE> implements TimestampGenerator<TIMESTAMP_TYPE>, Plugin {
    @Override
    public void register() {
        Act.dbManager().register(this);
    }
}
