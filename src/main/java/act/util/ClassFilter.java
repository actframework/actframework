package act.util;

/**
 * Defines class filter specification and handle method when
 * the class been found. <b>Note</b> only public and non-abstract
 * class will be filtered out, these two requirements are
 * implicit specification
 */
public abstract class ClassFilter<T> {

    /**
     * Once a class has been found as per the requirements
     * of this class filter, Act will load the class and call
     * this method on this filter instance
     *
     * @param clazz the class instance been found
     */
    public abstract void found(Class<? extends T> clazz);

    /**
     * Specify the super type that must be extended or implemented
     * by the targeting class
     */
    public abstract Class<T> superType();

}
