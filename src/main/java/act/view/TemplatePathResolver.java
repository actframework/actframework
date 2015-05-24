package act.view;

import org.osgl._;
import org.osgl.http.H;
import act.app.AppContext;
import org.osgl.util.E;
import org.osgl.util.S;

/**
 * Resolve template path for {@link AppContext}
 */
public class TemplatePathResolver extends _.Transformer<AppContext, String> {
    @Override
    public final String transform(AppContext context) {
        return resolve(context);
    }

    public final String resolve(AppContext context) {
        String path = context.templatePath();
        return resolveTemplatePath(path, context);
    }

    /**
     * Sub class shall use this method to implement template path resolving logic
     */
    protected String resolveTemplatePath(String path, AppContext context) {
        if (path.contains(".")) {
            return path;
        }
        H.Format fmt = context.format();
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
                throw E.unsupport("Request format not supported: %s", fmt);
        }
    }
}
