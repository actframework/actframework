package act.app;

import act.util.AnnotatedClassFinder;
import act.util.AnnotatedTypeFinder;
import act.util.SubClassFinder;
import act.util.SingletonBase;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;

import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;

/**
 * Find all classes annotated with {@link javax.inject.Singleton}
 */

public class SingletonFinder {

    private SingletonFinder() {}

    @SubClassFinder(SingletonBase.class)
    @AnnotatedClassFinder(Singleton.class)
    public static void found(Class<?> cls) {
        App.instance().registerSingletonClass(cls);
    }

}

