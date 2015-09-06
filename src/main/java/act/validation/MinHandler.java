package act.validation;

import act.ActComponent;
import act.app.ActionContext;
import act.controller.ActionMethodParamAnnotationHandlerPlugin;
import org.osgl.util.C;
import org.osgl.util.E;

import javax.validation.constraints.Min;
import java.lang.annotation.Annotation;
import java.util.Set;

@ActComponent
public class MinHandler extends ActionMethodParamAnnotationHandlerPlugin {
    @Override
    public Set<Class<? extends Annotation>> listenTo() {
        Set<Class<? extends Annotation>> set = C.newSet();
        set.add(Min.class);
        return set;
    }

    @Override
    public void handle(String paramName, Object paramVal, Annotation annotation, ActionContext context) {
        if (null == paramVal) {
            return;
        }
        long num = toLong(paramVal);
        Min theAnno = (Min) annotation;
        long limit = theAnno.value();
        if (limit > num) {
            context.addViolation(new ActionMethodParamConstraintViolation<Object>(paramVal, theAnno.message(), theAnno, context));
        }
    }

    private long toLong(Object val) {
        if (val instanceof Number) {
            return ((Number) val).longValue();
        } else {
            throw E.unexpected("Invalid object type found. Expected: Number, found: %s", val.getClass());
        }
    }
}
