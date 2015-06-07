package act.util;

import java.lang.annotation.Annotation;

public abstract class DescendantClassFilter<SUPER_TYPE> extends ClassFilter<SUPER_TYPE, Annotation> {

    public DescendantClassFilter() {
        super();
    }

    public DescendantClassFilter(Class<SUPER_TYPE> superType) {
        super(superType, null);
    }
}
