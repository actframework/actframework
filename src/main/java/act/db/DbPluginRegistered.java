package act.db;

import act.event.ActEvent;
import act.event.SystemEvent;

public class DbPluginRegistered extends ActEvent<DbPlugin> implements SystemEvent {
    public DbPluginRegistered(DbPlugin source) {
        super(source);
    }
}
