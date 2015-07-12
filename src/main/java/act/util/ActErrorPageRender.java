package act.util;

import act.Act;
import act.app.AppContext;
import act.view.Template;
import act.view.ViewManager;
import org.osgl.http.H;
import org.osgl.mvc.ErrorPageRenderer;
import org.osgl.mvc.result.ErrorResult;

public class ActErrorPageRender extends ErrorPageRenderer {

    public static final String ARG_ERROR = "_error";

    @Override
    protected String renderTemplate(ErrorResult error, H.Format format) {
        AppContext context = AppContext.current();

        context.templatePath(templatePath(error, context));
        ViewManager vm = Act.viewManager();
        Template t = vm.load(context);
        context.renderArg(ARG_ERROR, error);
        return null != t ? t.render(context) : null;
    }

    private String templatePath(ErrorResult result, AppContext context) {
        return context.config().errorTemplatePathResolver().resolve(result, context);
    }
}
