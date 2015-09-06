package act.validation;

import act.app.ActionContext;
import act.controller.Controller;
import org.hibernate.validator.internal.engine.MessageInterpolatorContext;

import javax.validation.*;
import javax.validation.metadata.ConstraintDescriptor;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ActionMethodParamConstraintViolation<T> implements ConstraintViolation<T> {

    private String msgTmpl;
    private String interpolatedMsg;
    private ConstraintDescriptor constraint;
    private ActionContext context;

    public ActionMethodParamConstraintViolation(Object validatedValue, String messageTemplate, Annotation annotation, ActionContext context) {
        this.msgTmpl = messageTemplate;
        this.interpolatedMsg = context.config().validationMessageInterpolator().interpolate(messageTemplate, createContext(validatedValue, annotation));
        this.constraint = new ActionMethodParamConstraintDescriptor(annotation);
        this.context = context;
    }

    @Override
    public String getMessage() {
        return interpolatedMsg;
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

    private MessageInterpolator.Context createContext(Object validatedValue, Annotation anno) {
        ConstraintDescriptor desc = new ActionMethodParamConstraintDescriptor(anno);
        return new MessageInterpolatorContext(desc, validatedValue, Object.class, desc.getAttributes());
    }

    private static class ValidationContext implements MessageInterpolator.Context {



        @Override
        public ConstraintDescriptor<?> getConstraintDescriptor() {
            return null;
        }

        @Override
        public Object getValidatedValue() {
            return null;
        }

        @Override
        public <T> T unwrap(Class<T> type) {
            return null;
        }

    }

}
