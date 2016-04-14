package act.util;

import org.osgl.util.E;

/**
 * Application could extend this class to create singleton classes.
 * <p>
 *     Note, the sub type must NOT be abstract and has public constructor
 * </p>
 */
public abstract class SingletonBase extends DestroyableBase {

    /**
     * Returns the singleton instance of the sub type
     * @param <T> the sub type of {@code SingletonBase}
     * @return the instance
     */
    public static <T> T instance() {
        throw E.tbd("This method will be enhanced on sub type of SingletonBase class");
    }

}
