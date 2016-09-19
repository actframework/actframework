package act.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate on a controller action handler to indicate the JSON result shall be
 * rendered with FastJson Circular Reference check disabled
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DisableFastJsonCircularReferenceDetect {
    ThreadLocal<Boolean> option = new ThreadLocal<Boolean>();
}
