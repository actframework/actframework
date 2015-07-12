package act.util;

import act.app.AppContext;
import org.osgl.http.H;
import org.osgl.mvc.result.ErrorResult;
import org.osgl.util.S;

public interface ErrorTemplatePathResolver {
    String resolve(ErrorResult result, AppContext context);

    public static class DefaultErrorTemplatePathResolver implements ErrorTemplatePathResolver {
        @Override
        public String resolve(ErrorResult result, AppContext context) {
            int code = result.statusCode();
            String suffix = "html";
            H.Format fmt = context.accept();
            switch (fmt) {
                case json:
                case html:
                case xml:
                    suffix = fmt.name();
                    break;
                default:
                    suffix = H.Format.txt.name();
            }
            return S.fmt("/error/e%s.%s", code, suffix);
        }
    }
}
