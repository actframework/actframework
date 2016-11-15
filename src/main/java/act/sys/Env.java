package act.sys;

import act.Act;
import org.osgl.$;
import org.osgl.util.C;
import org.rythmengine.utils.S;

import java.lang.annotation.*;

/**
 * Mark a module should only be loaded in certain environment
 */
public final class Env {

    private Env() {}

    /**
     * Used to mark a dependency injector module that
     * should be load only in specified profile.
     *
     * This annotation shall NOT used along with
     * {@link Mode} and {@link Group}
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
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
     * Used to mark a dependency injector module that
     * should be load only in specified node group
     *
     * This annotation shall NOT used along with
     * {@link Mode} and {@link Profile}
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Group {

        /**
         * The node group specification
         */
        String value();

        /**
         * If unless is `true` then the module should be load
         * unless the current node group is the value specified
         */
        boolean unless() default false;
    }

    /**
     * Used to mark a dependency injector module
     * that should be load only in specified mode
     *
     * This annotation shall NOT used along with
     * {@link Profile} and {@link Group}
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
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

    public static boolean matches(Mode modeTag) {
        return modeTag.unless() ^ (modeTag.value() == Act.mode());
    }

    public static boolean modeMatches(Act.Mode mode) {
        return mode == Act.mode();
    }

    public static boolean matches(Profile profileTag) {
        return profileTag.unless() ^ S.eq(profileTag.value(), Act.profile(), S.IGNORECASE);
    }

    public static boolean profileMatches(String profile) {
        return S.eq(profile, Act.profile(), S.IGNORECASE);
    }

    public static boolean matches(Group groupTag) {
        return groupTag.unless() ^ (S.eq(groupTag.value(), Act.nodeGroup(), S.IGNORECASE));
    }

    public static boolean groupMatches(String group) {
        return S.eq(group, Act.nodeGroup(), S.IGNORECASE);
    }

    private static final C.Set<Class<? extends Annotation>> ENV_ANNOTATION_TYPES = C.set(
            Env.Mode.class, Env.Profile.class, Env.Group.class
    );

    public static boolean isEnvAnnotation(Class<? extends Annotation> type) {
        return ENV_ANNOTATION_TYPES.contains(type);
    }


}
