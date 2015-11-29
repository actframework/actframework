package act.validation;

import act.ActComponent;
import act.app.ActionContext;
import act.controller.ActionMethodParamAnnotationHandlerPlugin;
import org.osgl.util.C;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.util.Set;

@ActComponent
public class NotBlankHandler extends ActionMethodParamAnnotationHandlerPlugin {

    @Override
    public Set<Class<? extends Annotation>> listenTo() {
        Set<Class<? extends Annotation>> set = C.newSet();
        set.add(NotBlank.class);
        return set;
    }

    @Override
    public void handle(String paramName, Object paramVal, Annotation annotation, ActionContext context) {
        if (S.isBlank(S.string(paramVal))) {
            NotBlank notBlank = (NotBlank) annotation;
            context.addViolation(new ActionMethodParamConstraintViolation<Object>(paramVal, notBlank.message(), notBlank, context));
        }
    }

}
