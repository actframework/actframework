package act.util;

import java.lang.annotation.Annotation;

public abstract class AnnotatedClassFilter<ANNOTATION_TYPE extends Annotation> extends ClassFilter<Object, ANNOTATION_TYPE> {

    public AnnotatedClassFilter() {
        super();
    }

    public AnnotatedClassFilter(Class<ANNOTATION_TYPE> annotationType) {
        super(null, annotationType);
    }
}
