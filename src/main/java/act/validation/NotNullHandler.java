package act.validation;

import act.app.ActionContext;
import act.controller.ActionMethodParamAnnotationHandlerPlugin;
import org.osgl.util.C;

import javax.validation.constraints.NotNull;
import java.lang.annotation.Annotation;
import java.util.Set;

public class NotNullHandler extends ActionMethodParamAnnotationHandlerPlugin {
    @Override
    public Set<Class<? extends Annotation>> listenTo() {
        Set<Class<? extends Annotation>> set = C.newSet();
        set.add(NotNull.class);
        return set;
    }

    @Override
    public void handle(String paramName, Object paramVal, Annotation annotation, ActionContext context) {
        if (null == paramVal) {
            NotNull notNull = (NotNull) annotation;
            context.addViolation(new ActionMethodParamConstraintViolation<Object>(paramVal, notNull.message(), notNull, context));
        }
    }
}
