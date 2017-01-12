package act;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a class as ActFramework component. An Actframework component will
 * be load directly into class loader when application starts up
 *
 * This is an Obsolete annotation. DO NOT USE IT!
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Deprecated
public @interface ActComponent {
}
