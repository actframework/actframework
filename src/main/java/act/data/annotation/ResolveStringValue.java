package act.data.annotation;

import org.osgl.util.StringValueResolver;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Mark an annotation as an indirect {@link org.osgl.util.StringValueResolver} tag
 */
@Documented
@Retention(RUNTIME)
@Target({ANNOTATION_TYPE, FIELD, PARAMETER})
public @interface ResolveStringValue {
    /**
     * Specify the supported {@link StringValueResolver} classes. By providing an array of
     * `StringValueResolver` classes we can reuse a certain annotation to target multiple types.
     *
     * E.g. `ReadContent` annotation can be used to denote both `String.class1` and `List<String>.class`
     * as the target type
     *
     * @return an array of {@link StringValueResolver} sub classes
     */
    Class<? extends StringValueResolver>[] value();
}
