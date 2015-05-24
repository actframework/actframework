package act.view;

import act.app.AppContext;

/**
 * A Template represents a resource that can be merged with {@link AppContext application context}
 * and output the result
 */
public interface Template {
    void merge(AppContext context);
}
