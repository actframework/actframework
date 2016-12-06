package act.sys;

import act.Act;
import org.osgl.$;
import org.osgl.util.C;
import org.rythmengine.utils.S;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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

    // See http://stackoverflow.com/questions/534648/how-to-daemonize-a-java-program
    public static class PID {

        private static String pid = getPid();

        private static String getPid() {
            File proc_self = new File("/proc/self");
            if(proc_self.exists()) try {
                return proc_self.getCanonicalFile().getName();
            }
            catch(Exception e) {
                /// Continue on fall-back
            }
            File bash = new File("/bin/bash");
            if(bash.exists()) {
                ProcessBuilder pb = new ProcessBuilder("/bin/bash","-c","echo $PPID");
                try {
                    Process p = pb.start();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    return rd.readLine();
                }
                catch(IOException e) {
                    return String.valueOf(Thread.currentThread().getId());
                }
            }
            // This is a cop-out to return something when we don't have BASH
            return String.valueOf(Thread.currentThread().getId());
        }

        public static String get() {
            return pid;
        }
    }

}
