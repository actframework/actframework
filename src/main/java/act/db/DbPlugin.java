package act.db;

import act.Act;
import act.app.App;
import act.plugin.Plugin;
import act.util.DestroyableBase;

import java.util.Map;

/**
 * The base class for Database Plugin
 */
public abstract class DbPlugin extends DestroyableBase implements Plugin {
    @Override
    public void register() {
        Act.dbManager().register(this);
        Act.trigger(new DbPluginRegistered(this));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || null != obj && getClass() == obj.getClass();
    }

    public abstract DbService initDbService(String id, App app, Map<String, Object> conf);

    public void afterDbServiceLoaded() {
    }

}
