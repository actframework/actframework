package act.util;

import act.Act;
import act.app.ActionContext;
import act.view.Template;
import act.view.ViewManager;
import com.alibaba.fastjson.JSON;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.mvc.ErrorPageRenderer;
import org.osgl.mvc.MvcConfig;
import org.osgl.mvc.result.ErrorResult;
import org.osgl.util.C;
import org.osgl.util.IO;

import java.util.HashMap;
import java.util.Map;

public class ActErrorPageRender extends ErrorPageRenderer {

    public static final String ARG_ERROR = "_error";

    private volatile Boolean i18n;

    private Map<String, $.Var<Template>> templateCache = new HashMap<>();

    @Override
    protected String renderTemplate(ErrorResult error, H.Format format) {
        ActionContext context = ActionContext.current();
        if (null == context) {
            return null;
        }
        int code = error.statusCode();
        Template t = getTemplate(code, context);
        if (null == t) {
            if (context.acceptJson()) {
                String errorMsg = error.getMessage();
                if (i18n()) {
                    errorMsg = context.i18n(errorMsg);
                }
                Map<String, Object> params = C.newMap("code", code, "message", errorMsg);
                return JSON.toJSONString(params);
            }
            return null;
        }
        context.renderArg(ARG_ERROR, error);
        return t.render(context);
    }

    private String templatePath(int code, ActionContext context) {
        ErrorTemplatePathResolver resolver = context.config().errorTemplatePathResolver();
        if (null == resolver) {
            resolver = new ErrorTemplatePathResolver.DefaultErrorTemplatePathResolver();
        }
        return resolver.resolve(code, context.accept());
    }

    private Template getTemplate(int code, ActionContext context) {
        H.Format format = context.accept();
        String key = code + "" + format;
        $.Var<Template> templateBag = templateCache.get(key);
        if (null == templateBag) {
            ViewManager vm = Act.viewManager();
            if (null == vm) {
                // unit testing
                return null;
            }
            context.templatePath(templatePath(code, context));
            Template t = vm.load(context);
            templateBag = $.var(t);
            templateCache.put(key, templateBag);
        }
        return templateBag.get();
    }

    private boolean i18n() {
        if (null == i18n) {
            synchronized (this) {
                if (null == i18n) {
                    i18n = Act.appConfig().i18nEnabled();
                }
            }
        }
        return i18n;
    }
}
