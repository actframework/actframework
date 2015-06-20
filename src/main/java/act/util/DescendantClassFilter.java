package act.util;

import java.lang.annotation.Annotation;

public abstract class DescendantClassFilter<SUPER_TYPE> extends ClassFilter<SUPER_TYPE, Annotation> {

    public DescendantClassFilter(Class<SUPER_TYPE> superType) {
        super(superType, null);
    }

    public DescendantClassFilter(boolean publicOnly, boolean noAbstract, Class<SUPER_TYPE> superType) {
        super(publicOnly, noAbstract, superType, null);
    }
}
