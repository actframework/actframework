package act.app;

import act.app.conf.AutoConfig;
import act.app.event.AppEventId;
import act.event.AppEventListenerBase;
import act.util.AnnotatedTypeFinder;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;

import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.util.EventObject;
import java.util.Map;
import java.util.Set;

/**
 * Find all classes annotated with {@link javax.inject.Singleton}
 */
public class SingletonFinder extends AnnotatedTypeFinder {
    public SingletonFinder() {
        super(Singleton.class, new $.F2<App, String, Map<Class<? extends AppByteCodeScanner>, Set<String>>>() {
            @Override
            public Map<Class<? extends AppByteCodeScanner>, Set<String>> apply(final App app, final String className) throws NotAppliedException, $.Break {
                app.singletonRegistry().register($.classForName(className, app.classLoader()));
                return null;
            }
        });
    }
}
