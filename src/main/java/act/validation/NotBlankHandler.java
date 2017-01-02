package act.validation;

import act.app.ActionContext;
import act.controller.ActionMethodParamAnnotationHandlerPlugin;
import org.osgl.util.C;
import org.osgl.util.S;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.annotation.Annotation;
import java.util.Set;

public class NotBlankHandler extends ActionMethodParamAnnotationHandlerPlugin implements ConstraintValidator<NotBlank, CharSequence> {

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

    @Override
    public void initialize(NotBlank constraintAnnotation) {
    }


    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return null != value && value.toString().trim().length() > 0;
    }
}
