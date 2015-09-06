package act.validation;

import act.ActComponent;
import act.app.ActionContext;
import act.controller.ActionMethodParamAnnotationHandlerPlugin;
import org.osgl.util.C;
import org.osgl.util.E;

import javax.validation.constraints.Max;
import java.lang.annotation.Annotation;
import java.util.Set;

@ActComponent
public class MaxHandler extends ActionMethodParamAnnotationHandlerPlugin {
    @Override
    public Set<Class<? extends Annotation>> listenTo() {
        Set<Class<? extends Annotation>> set = C.newSet();
        set.add(Max.class);
        return set;
    }

    @Override
    public void handle(String paramName, Object paramVal, Annotation annotation, ActionContext context) {
        if (null == paramVal) {
            return;
        }
        long num = toLong(paramVal);
        Max theAnno = (Max) annotation;
        long limit = theAnno.value();
        if (limit < num) {
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
