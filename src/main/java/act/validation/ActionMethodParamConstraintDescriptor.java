package act.validation;

import org.osgl.util.C;
import org.osgl.util.E;

import javax.validation.ConstraintTarget;
import javax.validation.ConstraintValidator;
import javax.validation.Payload;
import javax.validation.constraints.*;
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
        if (anno instanceof Size) {
            Size size = (Size) anno;
            return C.map("min", size.min(), "max", size.max());
        } else if (anno instanceof Max) {
            Max max = (Max) anno;
            return C.map("value", max.value());
        } else if (anno instanceof Min) {
            Min min = (Min) anno;
            return C.map("value", min.value());
        } else if (anno instanceof Pattern) {
            Pattern ptn = (Pattern) anno;
            return C.map("regexp", ptn.regexp());
        } else if (anno instanceof Digits) {
            Digits digits = (Digits) anno;
            return C.map("integer", digits.integer(), "fraction", digits.fraction());
        } else if (anno instanceof DecimalMax) {
            DecimalMax max = (DecimalMax) anno;
            return C.map("value", max.value());
        } else if (anno instanceof DecimalMin) {
            DecimalMin min = (DecimalMin) anno;
            return C.map("value", min.value());
        }

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
