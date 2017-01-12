package act.validation;

import act.app.ActionContext;
import act.controller.ActionMethodParamAnnotationHandlerPlugin;
import org.osgl.util.C;
import org.osgl.util.S;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.annotation.Annotation;
import java.util.Set;

public class EmailHandler extends ActionMethodParamAnnotationHandlerPlugin implements ConstraintValidator<Email, CharSequence> {
    @Override
    public Set<Class<? extends Annotation>> listenTo() {
        Set<Class<? extends Annotation>> set = C.newSet();
        set.add(Email.class);
        return set;
    }

    @Override
    public void handle(String paramName, Object paramVal, Annotation annotation, ActionContext context) {
        if (!isValid(paramVal)) {
            Email email = (Email) annotation;
            context.addViolation(new ActionMethodParamConstraintViolation<Object>(paramVal, email.message(), email, context));
        }
     }

    @Override
    public void initialize(Email email) {
    }

    @Override
    public boolean isValid(CharSequence charSequence, ConstraintValidatorContext constraintValidatorContext) {
        return isValid(charSequence);
    }

    private boolean isValid(Object val) {
        String s = S.string(val);
        return (S.isBlank(s) || s.toLowerCase().matches("^[_a-z0-9-']+(\\.[_a-z0-9-']+)*(\\+[0-9]+)?@[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,4})$"));
    }
}
