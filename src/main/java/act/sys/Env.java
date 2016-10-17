package act.sys;

import act.Act;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a module should only be loaded in certain environment
 */
public final class Env {

    private Env() {}

    /**
     * Used to mark a dependency injector module that
     * should be load only in specified profile
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Profile {

        /**
         * The profile specification
         */
        String value();

        /**
         * If unless is `true` then the module should be load
         * unless the current profile is the value specified
         */
        boolean unless() default false;
    }

    /**
     * Used to mark a dependency injector module
     * that should be load only in specified mode
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Mode {

        /**
         * The mode specification
         */
        Act.Mode value();

        /**
         * If unless is `true` then the module should be load
         * unless the current mode is the value specified
         */
        boolean unless() default false;
    }


}
