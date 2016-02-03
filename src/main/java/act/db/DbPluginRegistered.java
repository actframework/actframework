package act.db;

import act.event.ActEvent;

public class DbPluginRegistered extends ActEvent<DbPlugin> {
    public DbPluginRegistered(DbPlugin source) {
        super(source);
    }
}
