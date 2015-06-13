package act.controller;

import act.Act;
import act.app.AppContext;
import act.plugin.Plugin;

import java.lang.annotation.Annotation;
import java.util.Set;

public abstract class ActionMethodParamAnnotationHandlerPlugin implements Plugin, ActionMethodParamAnnotationHandler {
    @Override
    public void register() {
        Act.pluginManager().register(ActionMethodParamAnnotationHandler.class, this);
    }
}
