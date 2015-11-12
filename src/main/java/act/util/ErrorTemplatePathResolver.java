package act.util;

import act.app.ActionContext;
import org.osgl.http.H;
import org.osgl.mvc.result.ErrorResult;
import org.osgl.util.S;

import static org.osgl.http.H.Format.*;

public interface ErrorTemplatePathResolver {
    String resolve(ErrorResult result, ActionContext context);

    public static class DefaultErrorTemplatePathResolver implements ErrorTemplatePathResolver {
        @Override
        public String resolve(ErrorResult result, ActionContext context) {
            int code = result.statusCode();
            String suffix;
            H.Format fmt = context.accept();
            if (JSON == fmt || HTML == fmt || XML == fmt) {
                suffix = fmt.name();
            } else {
                suffix = TXT.name();
            }
            return S.fmt("/error/e%s.%s", code, suffix);
        }
    }
}
