package act.inject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a class/interface to be subject to auto bind process.
 *
 * ActFramework scans for class marked as `@AutoBind` and search for available
 * implementation/sub classes.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoBind {
}
