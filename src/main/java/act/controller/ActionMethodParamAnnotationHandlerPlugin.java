package act.controller;

import act.Act;
import act.plugin.Plugin;

public abstract class ActionMethodParamAnnotationHandlerPlugin implements Plugin, ActionMethodParamAnnotationHandler {
    @Override
    public void register() {
        Act.pluginManager().register(ActionMethodParamAnnotationHandler.class, this);
    }
}
