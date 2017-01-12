package act.util;

import java.lang.annotation.Annotation;

public abstract class AnnotatedClassFilter<ANNOTATION_TYPE extends Annotation> extends ClassFilter<Object, ANNOTATION_TYPE> {

    public AnnotatedClassFilter(Class<ANNOTATION_TYPE> annotationType) {
        super(null, annotationType);
    }

    public AnnotatedClassFilter(boolean publicOnly, boolean noAbstract, Class<ANNOTATION_TYPE> annotationType) {
        super(publicOnly, noAbstract, null, annotationType);
    }
}
