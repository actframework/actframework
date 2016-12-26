package act.validation;

import act.ActComponent;
import act.app.ActionContext;
import act.controller.ActionMethodParamAnnotationHandlerPlugin;
import org.osgl.util.C;

import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * @author Genko Lee
 */
@ActComponent
public class ValidHandler extends ActionMethodParamAnnotationHandlerPlugin {

    private static Validator validator;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    public Set<Class<? extends Annotation>> listenTo() {
        Set<Class<? extends Annotation>> set = C.newSet();
        set.add(Valid.class);
        return set;
    }

    public void handle(String paramName, Object paramVal, Annotation annotation, ActionContext context) {
        if (null == paramVal) {
            return;
        }

        Set constraintViolations = validator.validate(paramVal);
        if (!constraintViolations.isEmpty()) {
            context.addViolations(constraintViolations);
        }
    }

}
