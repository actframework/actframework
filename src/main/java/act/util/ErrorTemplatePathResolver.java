package act.util;

import act.Act;
import act.app.ActionContext;
import org.osgl.http.H;
import org.osgl.mvc.result.ErrorResult;
import org.osgl.util.S;

import static org.osgl.http.H.Format.*;

public interface ErrorTemplatePathResolver {
    String resolve(int code, H.Format format);

    class DefaultErrorTemplatePathResolver implements ErrorTemplatePathResolver {
        @Override
        public String resolve(int code, H.Format fmt) {
            String suffix;
            if (JSON == fmt || HTML == fmt || XML == fmt) {
                suffix = fmt.name();
            } else {
                suffix = TXT.name();
            }
            return Act.isProd() || "json".equals(suffix) ? S.fmt("/error/e%s.%s", code, suffix) : S.fmt("/error/dev/e%s.%s", code, suffix);
        }
    }
}
