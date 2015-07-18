package act.controller;

import act.app.ActionContext;

import java.lang.annotation.Annotation;
import java.util.Set;

public interface ActionMethodParamAnnotationHandler {
    Set<Class<? extends Annotation>> listenTo();
    void handle(String paramName, Object paramVal, Annotation annotation, ActionContext context);
}
