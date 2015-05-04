package org.osgl.oms.view;

import org.osgl._;
import org.osgl.oms.app.AppContext;

/**
 * Resolve template path for {@link org.osgl.oms.app.AppContext}
 */
public class TemplatePathResolver extends _.Transformer<AppContext, String> {
    @Override
    public final String transform(AppContext context) {
        return resolve(context);
    }

    public final String resolve(AppContext context) {
        String path = context.actionPath().replace('.', '/');
        return resolveTemplatePath(path, context);
    }

    /**
     * Sub class shall use this method to implement template path resolving logic
     */
    protected String resolveTemplatePath(String path, AppContext context) {
        return path;
    }
}
