package act.di;

import java.lang.annotation.*;

/**
 * Used to tag an annotation as {@link BeanLoader bean loader}
 * specification
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface BeanLoaderTag {
    /**
     * Specify the {@link BeanLoader} implementation used to
     * load bean(s)
     * @return the `BeanLoader` implementation
     */
    Class<? extends BeanLoader> value();

}
