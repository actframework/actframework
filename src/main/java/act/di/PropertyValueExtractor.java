package act.di;

import org.osgl.$;

/**
 * Implement {@link ValueExtractor} by extracting value from a bean
 * by property provided
 */
public class PropertyValueExtractor implements ValueExtractor {

    @Override
    public Object get(Object bean, String property) {
        return $.getProperty(bean, property);
    }
}
