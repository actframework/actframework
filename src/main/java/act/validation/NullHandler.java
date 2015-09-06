package act.validation;

import act.ActComponent;
import act.app.ActionContext;
import act.controller.ActionMethodParamAnnotationHandlerPlugin;
import org.osgl.util.C;

import javax.validation.constraints.Null;
import java.lang.annotation.Annotation;
import java.util.Set;

@ActComponent
public class NullHandler extends ActionMethodParamAnnotationHandlerPlugin {
    @Override
    public Set<Class<? extends Annotation>> listenTo() {
        Set<Class<? extends Annotation>> set = C.newSet();
        set.add(Null.class);
        return set;
    }

    @Override
    public void handle(String paramName, Object paramVal, Annotation annotation, ActionContext context) {
        if (null != paramVal) {
            Null notNull = (Null) annotation;
            context.addViolation(new ActionMethodParamConstraintViolation<Object>(paramVal, notNull.message(), notNull, context));
        }
    }
}
