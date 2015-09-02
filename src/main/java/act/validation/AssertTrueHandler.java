package act.validation;

import act.ActComponent;
import act.app.ActionContext;
import act.controller.ActionMethodParamAnnotationHandlerPlugin;
import org.osgl.util.C;

import javax.validation.constraints.AssertTrue;
import java.lang.annotation.Annotation;
import java.util.Set;

@ActComponent
public class AssertTrueHandler extends ActionMethodParamAnnotationHandlerPlugin {

    @Override
    public Set<Class<? extends Annotation>> listenTo() {
        Set<Class<? extends Annotation>> set = C.newSet();
        set.add(AssertTrue.class);
        return set;
    }

    @Override
    public void handle(String paramName, Object paramVal, Annotation annotation, ActionContext context) {
        if (null != paramVal && !Boolean.parseBoolean(paramVal.toString())) {
            AssertTrue theAnno = (AssertTrue) annotation;
            context.addViolation(new ActionMethodParamConstraintViolation<Object>(theAnno.message(), theAnno, context));
        }
    }

}
