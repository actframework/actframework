package act.validation;

import act.app.ActionContext;
import act.controller.Controller;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import java.lang.annotation.Annotation;

public class ActionMethodParamConstraintViolation<T> implements ConstraintViolation<T> {

    private String msgTmpl;
    private String interpolatedMsg;
    private ConstraintDescriptor constraint;
    private ActionContext context;

    public ActionMethodParamConstraintViolation(String messageTemplate, /*String interpolatedMsg,*/ Annotation annotation, ActionContext context) {
        this.msgTmpl = messageTemplate;
        //this.interpolatedMsg = interpolatedMsg;
        this.constraint = new ActionMethodParamConstraintDescriptor(annotation);
        this.context = context;
    }

    @Override
    public String getMessage() {
        return msgTmpl;
    }

    @Override
    public String getMessageTemplate() {
        return msgTmpl;
    }

    @Override
    public T getRootBean() {
        return null;
    }

    @Override
    public Class getRootBeanClass() {
        return Controller.class;
    }

    @Override
    public Object getLeafBean() {
        return null;
    }

    @Override
    public Object[] getExecutableParameters() {
        return new Object[0];
    }

    @Override
    public Object getExecutableReturnValue() {
        return null;
    }

    @Override
    public Path getPropertyPath() {
        return null;
    }

    @Override
    public Object getInvalidValue() {
        return null;
    }

    @Override
    public ConstraintDescriptor<?> getConstraintDescriptor() {
        return constraint;
    }

    @Override
    public <U> U unwrap(Class<U> type) {
        return context.newInstance(type);
    }

}
