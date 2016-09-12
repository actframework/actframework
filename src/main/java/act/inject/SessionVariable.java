package act.inject;

import act.cli.CliSession;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate a binding from a {@link CliSession#attribute(String) CLI session variable} to
 * a commander field or parameter;
 *
 * Or binding from a {@link org.osgl.http.H.Session#get(String)} to a controller field or action handler
 * parameter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface SessionVariable {

    /**
     * Specifies the session attribute key
     */
    String value();
}
