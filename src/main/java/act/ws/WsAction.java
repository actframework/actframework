package act.ws;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@code WsAction} annotation is used to mark a
 * method (the action handler) that should be executed
 * to handle a websocket message
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface WsAction {

    /**
     * Returns the request paths that this
     * action mapped to.
     *
     * It is possible to use variable in the path
     */
    String[] value() default {};

}
