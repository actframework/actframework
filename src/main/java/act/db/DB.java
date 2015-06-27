package act.db;

import act.app.DbServiceManager;
import act.app.ProjectLayoutProbe;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used in multiple Database application to associate a DB engine with an
 * entity class
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface DB {
    /**
     * Specify the DB engine the entity of the class shall be landed.
     * <p>Default value: "default"</p>
     */
    String value() default DbServiceManager.DEFAULT;
}
