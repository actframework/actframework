package act.util;

import org.osgl.util.E;

import java.lang.annotation.Annotation;

/**
 * Defines class filter specification and handle method when
 * the class been found. <b>Note</b> only public and non-abstract
 * class will be filtered out, these two requirements are
 * implicit specification
 */
public abstract class ClassFilter<SUPER_TYPE, ANNOTATION_TYPE extends Annotation> {

    private Class<SUPER_TYPE> superType;
    private Class<ANNOTATION_TYPE> annotationType;

    public ClassFilter() {}

    public ClassFilter(Class<SUPER_TYPE> superType, Class<ANNOTATION_TYPE> annotationType) {
        E.npeIf(superType == null && annotationType == null);
        this.superType = superType;
        this.annotationType = annotationType;
    }

    /**
     * Once a class has been found as per the requirements
     * of this class filter, Act will load the class and call
     * this method on this filter instance
     *
     * @param clazz the class instance been found
     */
    public abstract void found(Class<? extends SUPER_TYPE> clazz);

    /**
     * Specify the super type that must be extended or implemented
     * by the targeting class
     */
    public Class<SUPER_TYPE> superType() {
        return superType;
    }

    /**
     * Specify the annotation type that target class will be annotated
     */
    public Class<ANNOTATION_TYPE> annotationType() {
        return annotationType;
    }

}
