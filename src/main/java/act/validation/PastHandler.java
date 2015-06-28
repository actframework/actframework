package act.validation;

import act.app.AppContext;
import act.controller.ActionMethodParamAnnotationHandlerPlugin;
import org.osgl._;
import org.osgl.util.C;

import javax.validation.constraints.Past;
import java.lang.annotation.Annotation;
import java.util.Date;
import java.util.Set;

public class PastHandler extends ActionMethodParamAnnotationHandlerPlugin {

    @Override
    public Set<Class<? extends Annotation>> listenTo() {
        Set<Class<? extends Annotation>> set = C.newSet();
        set.add(Past.class);
        return set;
    }

    @Override
    public void handle(String paramName, Object paramVal, Annotation annotation, AppContext context) {
        if (paramVal == null) return;
        if (paramVal instanceof Date) {
            Date date = (Date)paramVal;
            if (date.getTime() < _.ms()) return;
        }
        Past theAnno = (Past) annotation;
        context.addViolation(new ActionMethodParamConstraintViolation<Object>(theAnno.message(), theAnno, context));
    }

}
