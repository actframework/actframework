package org.osgl.oms.view;

import org.osgl.oms.OMS;
import org.osgl.oms.plugin.Plugin;

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
        OMS.viewManager().register(this);
    }
}
