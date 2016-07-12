package act.di;

import org.osgl.$;

import java.util.List;

/**
 * Define a generic interface to implement bean instance loading mechanisms
 */
public interface BeanLoader<T> {
    /**
     * Load a bean based on the `hint` specified.
     *
     * It is up to the implementation to decide how to use the `hint`
     *
     * @param hint the hint to specify the bean to be loaded
     * @param options optional parameters specified to refine the loading process
     * @return the bean instance
     */
    T load(Object hint, Object ... options);

    /**
     * Load multiple beans based on the `hint` specified.
     *
     * It is up to the implemetnation to decide how to use the `hint`
     *
     * @param hint the hint to specify the bean instances to be loaded
     * @param options optional parameters specified to refine the loading process
     * @return a list of bean instances
     */
    List<T> loadMultiple(Object hint, Object ... options);

    /**
     * Create a filter function with the hint and options specified. This could
     * be used to produce composite Bean loader based on other bean loaders
     * @param hint the hint to specify the bean instances to be loaded
     * @param options the optional parameters specified to refine the loading process
     * @return a filter to check if a certain bean instance matches this bean loader specification
     */
    $.Function<T, Boolean> filter(Object hint, Object ... options);
}
