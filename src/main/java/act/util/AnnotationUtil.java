package act.util;

import org.osgl.$;
import org.osgl.exception.UnexpectedException;

import java.lang.annotation.Annotation;

public class AnnotationUtil {
    public static <T extends Annotation> T declaredAnnotation(Class c, Class<T> annoClass) {
        Annotation[] aa = c.getDeclaredAnnotations();
        if (null == aa) {
            return null;
        }
        for (Annotation a : aa) {
            if (annoClass.isInstance(a)) {
                return (T) a;
            }
        }
        return null;
    }

    /**
     * Returns the {@link Annotation} tagged on another annotation instance
     * @param annotation the annotation instance
     * @param tagClass the expected annotation class
     * @param <T> the generic type of the expected annotation
     * @return the annotation tagged on annotation of type `tagClass`
     */
    public static <T extends Annotation> T annotation(Annotation annotation, Class<T> tagClass) {
        Class<?> c = classOf(annotation);
        for (Annotation a : c.getAnnotations()) {
            if (tagClass.isInstance(a)) {
                return (T) a;
            }
        }
        return null;
    }

    /**
     * Returns the class of an annotation instance
     * @param annotation the annotation instance
     * @param <T> the generic type of the annotation
     * @return the real annotation class
     */
    public static <T extends Annotation> Class<T> classOf(Annotation annotation) {
        Class<?>[] ca = annotation.getClass().getInterfaces();
        for (Class<?> c: ca) {
            if (Annotation.class.isAssignableFrom(c)) {
                return $.cast(c);
            }
        }
        throw new UnexpectedException("!!!");
    }
}
