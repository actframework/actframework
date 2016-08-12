package act.inject;

import java.lang.annotation.*;

/**
 * This annotation is used to inject information into a class
 * field, bean property or method parameter.
 *
 * <p>
 *     Refer to <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Context.html">javax.ws.rs.core.Context</a>
 * </p>
 *
 * @deprecated use `org.osgl.inject.annotation.Provided` instead
 * @see org.osgl.inject.annotation.Provided
 */
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Deprecated
public @interface Context {
}