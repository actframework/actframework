package act.handler;

import act.Act;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a class method as non-block method.
 *
 * When an action handler created from an non-block
 * method the handler will be treated as an {@link ExpressHandler}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NonBlock {
}
