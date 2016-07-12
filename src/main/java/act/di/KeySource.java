package act.di;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies how to extract key value for a Map injection from
 * a certain bean
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface KeySource {

    /**
     * Specify source of key value. Could be simply the
     * property
     */
    String value();

    /**
     * Specify the ValueExtractor implementation which will be
     * called when it needs to extract a value from a bean.
     *
     * By default the implementation is {@link PropertyValueExtractor}
     *
     * @return the extractor class.
     */
    Class<? extends ValueExtractor> extractor() default PropertyValueExtractor.class;
}
