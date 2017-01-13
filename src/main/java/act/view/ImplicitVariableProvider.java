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
     * into template render arguments for action view
     */
    public abstract List<ActionViewVarDef> implicitActionViewVariables();

    /**
     * Returns a list of implicit variables the plugin needs to inject
     * into template render arguments for mailer view
     */
    public abstract List<MailerViewVarDef> implicitMailerViewVariables();

    @Override
    public void register() {
        Act.viewManager().register(this);
    }
}
