// Code copied from Google Guice
package act.di;

/**
 * Listens for injections into instances of type {@code I}. Useful for performing further
 * injections, post-injection initialization, and more.
 *
 * @author crazybob@google.com (Bob Lee)
 * @author jessewilson@google.com (Jesse Wilson)
 * @since 2.0
 */
public interface DiListener<I> {

    /**
     * Invoked by Guice after it injects the fields and methods of instance.
     *
     * @param injectee instance that Guice injected dependencies into
     */
    void afterInjection(I injectee);
}
