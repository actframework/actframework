package act.di;

import act.di.loader.AnnotatedBeanLoader;
import act.di.loader.ConfigurationLoader;
import act.di.loader.SubClassBeanLoader;

import javax.inject.Qualifier;
import javax.inject.Scope;
import java.lang.annotation.*;

/**
 * `DI` is the namespace to encapsulate common used annotation definitions
 * that are relevant to Dependency Injection
 */
public final class DI {

    /**
     * Used to tag an annotation as {@link BeanLoader bean loader}
     * specification.
     *
     * The framework treats the annotation type that is annotated
     * with `DI.Loader` as {@link Qualifier} semantically
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.ANNOTATION_TYPE)
    @Qualifier
    public @interface Loader {
        /**
         * Specify the {@link BeanLoader} implementation used to
         * load bean(s)
         * @return the `BeanLoader` implementation
         */
        Class<? extends BeanLoader> value();
    }


    /**
     * Mark elements of a field or method parameter must be of type
     * specified or the generic type of the container
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Loader(SubClassBeanLoader.class)
    @Qualifier
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
    @Loader(AnnotatedBeanLoader.class)
    @Qualifier
    public @interface AnnotatedWith {
        /**
         * Specifies the annotation class which annotated
         * the class of the bean to be loaded
         *
         * @return the annotation class
         */
        Class<? extends Annotation> value();
    }

    /**
     * Mark elements of a field or method parameter should be injected
     * from the value provisioned in {@link act.conf.AppConfig application configuration}
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Loader(ConfigurationLoader.class)
    @Qualifier
    public @interface Configuation {
        /**
         * Specifies configuration key
         * @return the configuration key
         */
        String value();
    }

    /**
     * Mark a factory method of a module (any class) that can be used to
     * create bean instance. The factory method could be annotated with
     * {@link Qualifier} annotations like {@link javax.inject.Named} to provide
     * some differentiation to injection
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Provides {
    }

    /**
     * Mark a class whose instance, when get injected into program, should be
     * instantiated only once per user session
     *
     * @see Scope
     */
    @Scope
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface SessionScoped {
    }

    /**
     * Mark a class whose instance, when get injected into program, should be
     * instantiated only once per user request
     *
     * @see Scope
     */
    @Scope
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface RequestScoped {
    }

}
