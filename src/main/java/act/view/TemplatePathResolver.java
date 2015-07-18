package act.view;

import act.app.ActionContext;
import act.util.ActContext;
import org.osgl._;
import org.osgl.http.H;
import org.osgl.util.E;
import org.osgl.util.S;

/**
 * Resolve template path for {@link ActionContext}
 */
public class TemplatePathResolver extends _.Transformer<ActContext, String> {
    @Override
    public final String transform(ActContext context) {
        return resolve(context);
    }

    public final String resolve(ActContext context) {
        String path = context.templatePath();
        return resolveTemplatePath(path, context);
    }

    /**
     * Sub class shall use this method to implement template path resolving logic
     */
    protected String resolveTemplatePath(String path, ActContext context) {
        if (path.contains(".")) {
            return path;
        }
        H.Format fmt = context.accept();
        switch (fmt) {
            case html:
            case xml:
            case json:
            case txt:
            case csv:
                return S.builder(path).append(".").append(fmt.name()).toString();
            case unknown:
                return S.builder(path).append(".html").toString();
            default:
                throw E.unsupport("Request accept not supported: %s", fmt);
        }
    }
}
