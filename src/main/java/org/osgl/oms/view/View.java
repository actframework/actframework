package org.osgl.oms.view;

import org.osgl.oms.OMS;
import org.osgl.oms.plugin.Plugin;

/**
 * The base class that different View solution should extends
 */
public abstract class View implements Plugin {

    /**
     * Returns the View solution's name. Recommended name should
     * be in lower case characters. E.g. freemarker, velocity,
     * rythm etc
     */
    public abstract String name();

    @Override
    public void register() {
        OMS.viewManager().register(this);
    }
}
