package org.osgl.oms.view;

import org.osgl.oms.app.AppContext;

/**
 * A Template represents a resource that can be merged with {@link org.osgl.oms.app.AppContext application context}
 * and output the result
 */
public interface Template {
    void merge(AppContext context);
}
