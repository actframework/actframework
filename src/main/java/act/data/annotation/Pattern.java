package act.data.annotation;

import java.lang.annotation.*;

/**
 * Specify the Date time format
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD})
public @interface Pattern {
    /**
     * The date time format value. E.g
     *
     * * "yyyy-MM-dd"
     * * "dd/MM/yyyy HH:MM"
     *
     * @return the format string
     */
    String value();
}
