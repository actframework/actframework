package act.validation;

import act.app.AppContext;
import act.controller.ActionMethodParamAnnotationHandlerPlugin;
import org.osgl._;
import org.osgl.util.C;

import javax.validation.constraints.Future;
import java.lang.annotation.Annotation;
import java.util.Date;
import java.util.Set;

public class FutureHandler extends ActionMethodParamAnnotationHandlerPlugin {

    @Override
    public Set<Class<? extends Annotation>> listenTo() {
        Set<Class<? extends Annotation>> set = C.newSet();
        set.add(Future.class);
        return set;
    }

    @Override
    public void handle(String paramName, Object paramVal, Annotation annotation, AppContext context) {
        if (paramVal == null) return;
        if (paramVal instanceof Date) {
            Date date = (Date)paramVal;
            if (date.getTime() > _.ms()) return;
        }
        Future theAnno = (Future) annotation;
        context.addViolation(new ActionMethodParamConstraintViolation<Object>(theAnno.message(), theAnno, context));
    }

}
