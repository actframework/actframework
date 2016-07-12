package act.di;

import act.di.loader.AnnotatedBeanLoader;
import act.di.loader.SubClassBeanLoader;

import java.lang.annotation.*;

/**
 * `DI` is the namespace to encapsulate common used annotation definitions
 * that are relevant to Dependency Injection
 */
public final class DI {

    /**
     * Mark elements of a field or method parameter must be of type
     * specified or the generic type of the container
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @BeanLoaderTag(SubClassBeanLoader.class)
    public @interface TypeOf {

        /**
         * A placeholder type. If specified then it indicate framework
         * should use the generic type of the container element
         */
        final class ELEMENT_TYPE {}

        /**
         * Specify the class of the bean to be loaded into the target
         * container.
         *
         * Note the class specified here must be type of
         * the generic type of the container element unless it is
         * {@link ELEMENT_TYPE}.
         *
         * The default value is {@link ELEMENT_TYPE}
         *
         * @return the class of the bean to be loaded
         */
        Class<?> value() default ELEMENT_TYPE.class;
    }

    /**
     * Mark elements of a field or method parameter must be of type that are
     * annotated by certain annotation class. Implicit requirement of loading
     * beans is the bean's class must be type of the generic type of container
     * element
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @BeanLoaderTag(AnnotatedBeanLoader.class)
    public @interface AnnotatedBy {
        /**
         * Specifies the annotation class which annotated
         * the class of the bean to be loaded
         *
         * @return the annotation class
         */
        Class<? extends Annotation> value();
    }



}
