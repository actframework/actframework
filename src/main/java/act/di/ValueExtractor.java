package act.di;

/**
 * a `ValueExtractor` extract value from a bean
 */
public interface ValueExtractor {
    /**
     * Returns a value from the given bean instance
     * @param bean the bean instance
     * @param source specify the source of the value
     * @return the value extract from the bean instance
     */
    Object get(Object bean, String source);
}
