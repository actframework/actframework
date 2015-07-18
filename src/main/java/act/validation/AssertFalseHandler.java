package act.validation;

import act.app.ActionContext;
import act.controller.ActionMethodParamAnnotationHandlerPlugin;
import org.osgl.util.C;

import javax.validation.constraints.AssertFalse;
import java.lang.annotation.Annotation;
import java.util.Set;

public class AssertFalseHandler extends ActionMethodParamAnnotationHandlerPlugin {

    @Override
    public Set<Class<? extends Annotation>> listenTo() {
        Set<Class<? extends Annotation>> set = C.newSet();
        set.add(AssertFalse.class);
        return set;
    }

    @Override
    public void handle(String paramName, Object paramVal, Annotation annotation, ActionContext context) {
        if (null != paramVal && Boolean.parseBoolean(paramVal.toString())) {
            AssertFalse theAnno = (AssertFalse) annotation;
            context.addViolation(new ActionMethodParamConstraintViolation<Object>(theAnno.message(), theAnno, context));
        }
    }

}
