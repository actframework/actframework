package act.plugin;

import act.Act;
import act.app.App;

/**
 * The base class for Plugin that could be applied to a certain
 * application
 */
public abstract class AppServicePlugin implements Plugin {
    @Override
    public void register() {
        Act.appServicePluginManager().register(this);
    }

    abstract protected void applyTo(App app);
}
