package act.util;

import act.Act;
import act.app.ActionContext;
import act.view.Template;
import act.view.ViewManager;
import org.osgl.http.H;
import org.osgl.mvc.ErrorPageRenderer;
import org.osgl.mvc.result.ErrorResult;

public class ActErrorPageRender extends ErrorPageRenderer {

    public static final String ARG_ERROR = "_error";

    @Override
    protected String renderTemplate(ErrorResult error, H.Format format) {
        ActionContext context = ActionContext.current();
        if (null == context) {
            return null;
        }
        context.templatePath(templatePath(error, context));
        ViewManager vm = Act.viewManager();
        if (null == vm) {
            // unit testing
            return null;
        }
        Template t = vm.load(context);
        context.renderArg(ARG_ERROR, error);
        return null != t ? t.render(context) : null;
    }

    private String templatePath(ErrorResult result, ActionContext context) {
        ErrorTemplatePathResolver resolver = context.config().errorTemplatePathResolver();
        if (null == resolver) {
            resolver = new ErrorTemplatePathResolver.DefaultErrorTemplatePathResolver();
        }
        return resolver.resolve(result, context);
    }
}
