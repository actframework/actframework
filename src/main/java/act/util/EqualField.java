package act.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a field must be considered when generating {@link Object#equals(Object)}
 * and {@link Object#hashCode()} methods on the enclosing class.
 * @see Data
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface EqualField {
}
