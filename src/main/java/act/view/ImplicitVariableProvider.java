package act.view;

import act.Act;
import act.plugin.Plugin;

import java.util.List;

/**
 * Plugin developer could extend this interface to inject
 * implicit variables to view template
 */
public abstract class ImplicitVariableProvider implements Plugin {

    /**
     * Returns a list of implicit variables the plugin needs to inject
     * into template render arguments
     */
    public abstract List<VarDef> implicitVariables();

    @Override
    public void register() {
        Act.viewManager().register(this);
    }
}
