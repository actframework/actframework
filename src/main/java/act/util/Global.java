package act.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark an interceptor as global interceptor.
 *
 * ActFramework will register interceptor implementation automatically if it is marked
 * as `@Global`
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Global {
}
