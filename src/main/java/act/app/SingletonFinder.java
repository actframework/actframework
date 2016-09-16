package act.app;

import act.util.AnnotatedClassFinder;
import act.util.SingletonBase;
import act.util.SubClassFinder;

import javax.inject.Singleton;

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

