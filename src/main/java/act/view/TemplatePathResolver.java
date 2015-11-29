package act.view;

import act.app.ActionContext;
import act.util.ActContext;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.util.E;
import org.osgl.util.S;

import static org.osgl.http.H.Format.*;

/**
 * Resolve template path for {@link ActionContext}
 */
public class TemplatePathResolver extends $.Transformer<ActContext, String> {
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
        if (UNKNOWN == fmt) {
            fmt = HTML;
        }
        if (isAcceptFormatSupported(fmt)) {
            return S.builder(path).append(".").append(fmt.name()).toString();
        }
        throw E.unsupport("Request accept not supported: %s", fmt);
    }

    public static boolean isAcceptFormatSupported(H.Format fmt) {
        return (UNKNOWN == fmt || HTML == fmt || JSON == fmt || XML == fmt || TXT == fmt || CSV == fmt);
    }
}
