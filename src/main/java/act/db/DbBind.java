package act.db;

import act.db.di.FindBy;
import org.osgl.inject.annotation.LoadValue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation specify a field or parameter should be retrieved from
 * database through certain binding key with value get from the current
 * {@link act.util.ActContext}
 */
@LoadValue(FindBy.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface DbBind {
    /**
     * Specifies the db query field. Default value is an empty string
     * meaning the bind name (from {@literal @}Named, or field name
     * or param name) will be used as db query field
     * @return the db query field spec
     */
    String value() default "";

    /**
     * Indicate if it shall use the resolved value to search for ID or normal field
     * @return `true` if it shall bind by ID field, `false` otherwise
     */
    boolean byId() default true;

    /**
     * If {@link #byId()} is `false` then developer can use this field
     * to specify the type of the search field
     * @return the type of the search field
     */
    Class fieldType() default String.class;
}
