package act.validation;

import act.app.AppContext;
import act.controller.ActionMethodParamAnnotationHandlerPlugin;
import org.osgl.util.C;

import javax.validation.constraints.AssertTrue;
import java.lang.annotation.Annotation;
import java.util.Set;

public class AssertTrueHandler extends ActionMethodParamAnnotationHandlerPlugin {

    @Override
    public Set<Class<? extends Annotation>> listenTo() {
        Set<Class<? extends Annotation>> set = C.newSet();
        set.add(AssertTrue.class);
        return set;
    }

    @Override
    public void handle(String paramName, Object paramVal, Annotation annotation, AppContext context) {
        if (null != paramVal && !Boolean.parseBoolean(paramVal.toString())) {
            AssertTrue theAnno = (AssertTrue) annotation;
            context.addViolation(new ActionMethodParamConstraintViolation<Object>(theAnno.message(), theAnno, context));
        }
    }

}
