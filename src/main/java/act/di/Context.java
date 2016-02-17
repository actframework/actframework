package act.di;

import java.lang.annotation.*;

/**
 * This annotation is used to inject information into a class
 * field, bean property or method parameter.
 *
 * <p>
 *     Refer to <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Context.html">javax.ws.rs.core.Context</a>
 * </p>
 */
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Context {
}