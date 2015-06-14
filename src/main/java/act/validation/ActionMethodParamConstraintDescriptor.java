package act.validation;

import org.osgl.util.C;
import org.osgl.util.E;

import javax.validation.ConstraintTarget;
import javax.validation.ConstraintValidator;
import javax.validation.Payload;
import javax.validation.metadata.ConstraintDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ActionMethodParamConstraintDescriptor<T extends Annotation> implements ConstraintDescriptor<T> {

    private T anno;
    private Class<? extends Annotation> cls;

    public ActionMethodParamConstraintDescriptor(T annotation) {
        anno = annotation;
        cls = annotation.getClass();
    }

    @Override
    public T getAnnotation() {
        return anno;
    }

    private <T> T get(String methodName) {
        try {
            Method m = cls.getDeclaredMethod(methodName);
            return (T) m.invoke(anno);
        } catch (Exception e) {
            throw E.unexpected(e);
        }
    }

    private Set getSet(String methodName) {
        Object array = get(methodName);
        if (null == array) {
            return C.empty();
        }
        if (!array.getClass().isArray()) {
            throw E.unexpected("%s return type is not an array", methodName);
        }
        Set set = C.newSet();
        int len = Array.getLength(array);
        for (int i = 0; i < len; ++i) {
            set.add(Array.get(array, i));
        }
        return set;
    }

    @Override
    public String getMessageTemplate() {
        return get("message");
    }

    @Override
    public Set<Class<?>> getGroups() {
        return getSet("group");
    }

    @Override
    public Set<Class<? extends Payload>> getPayload() {
        return getSet("payload");
    }

    @Override
    public ConstraintTarget getValidationAppliesTo() {
        return ConstraintTarget.PARAMETERS;
    }

    @Override
    public List<Class<? extends ConstraintValidator<T, ?>>> getConstraintValidatorClasses() {
        return C.list();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return C.map();
    }

    @Override
    public Set<ConstraintDescriptor<?>> getComposingConstraints() {
        return C.empty();
    }

    @Override
    public boolean isReportAsSingleViolation() {
        return false;
    }
}
