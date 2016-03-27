package act.util;

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
}
