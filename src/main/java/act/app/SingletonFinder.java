package act.app;

import act.util.AnnotatedTypeFinder;
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

    public static class AnnotationFinder extends AnnotatedTypeFinder {
        public AnnotationFinder() {
            super(Singleton.class, new $.F2<App, String, Map<Class<? extends AppByteCodeScanner>, Set<String>>>() {
                @Override
                public Map<Class<? extends AppByteCodeScanner>, Set<String>> apply(final App app, final String className) throws NotAppliedException, $.Break {
                    app.registerSingletonClass($.classForName(className, app.classLoader()));
                    return null;
                }
            });
        }
    }

    public static class SubTypeFinder extends act.util.SubTypeFinder<SingletonBase> {
        public SubTypeFinder() {
            super(SingletonBase.class);
        }

        @Override
        protected void found(Class<? extends SingletonBase> target, App app) {
            app.registerSingletonClass(target);
        }
    }

}

