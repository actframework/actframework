package act.app.conf;

import java.lang.annotation.*;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.TYPE)
public @interface AutoConfig {
    /**
     * define namespace of the configuration
     */
    String value() default "app";
}
