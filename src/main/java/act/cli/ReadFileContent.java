package act.cli;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate if the parameter is a file path, then read the content of the file into the parameter,
 * otherwise leave the parameter as it is.
 *
 * Note this assumes the file be a ascii text file
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface ReadFileContent {
}
