package act.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a field shall be ignored when generating {@link Object#equals(Object)}
 * and {@link Object#hashCode()} methods on the enclosing class
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface EqualIgnore {
}
