package act.util;

import act.Act;
import act.app.ActionContext;
import act.view.Template;
import act.view.ViewManager;
import com.alibaba.fastjson.JSON;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.mvc.ErrorPageRenderer;
import org.osgl.mvc.MvcConfig;
import org.osgl.mvc.result.ErrorResult;

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
        Integer errorCode = error.errorCode();
        int statusCode = error.statusCode();
        Template t = getTemplate(statusCode, context);
        if (null == t) {
            String errorMsg = error.getMessage();
            if (null == errorMsg) {
                errorMsg = MvcConfig.errorMessage(error.status());
            }
            if (i18n()) {
                String translated = context.i18n(true, errorMsg);
                if (translated == errorMsg) {
                    translated = context.i18n(true, MvcConfig.class, errorMsg);
                }
                errorMsg = translated;
            }
            H.Format accept = context.accept();
            if (H.Format.JSON == accept) {
                return jsonContent(error, errorCode, errorMsg);
            } else if (H.Format.HTML == accept) {
                String header = "HTTP/1.1 " + statusCode + " " + errorMsg;
                String content = "<!DOCTYPE html><html><head><title>"
                        + header
                        + "</title></head><body><h1>"
                        + header + "</h1></body></html>";
                return content;
            } else if (H.Format.XML == accept) {
                StringBuilder sb = new StringBuilder();
                sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><error>");
                if (null != errorCode) {
                    sb.append("<code>").append(errorCode).append("</code");
                }
                sb.append("<message>").append(errorMsg).append("</message></error>");
                return sb.toString();
            } else if (H.Format.CSV == accept) {
                if (null == errorCode) {
                    return "message\n" + errorMsg;
                } else {
                    return "code,message\n" + errorCode + "," + errorMsg;
                }
            } else if (H.Format.TXT == accept) {
                return null == errorCode ? errorMsg : errorCode + " " + errorMsg;
            } else {
                Act.LOGGER.warn("Unsupported HTTP accept format[%s], will output error[%s] message using JSON format", accept, error);
                return jsonContent(error, errorCode, errorMsg);
            }
        }
        context.renderArg(ARG_ERROR, error);
        return t.render(context);
    }

    private String jsonContent(ErrorResult error, Integer errorCode, String errorMsg) {
        Object payload = error.attachment();
        if (null != payload) {
            return JSON.toJSONString(payload);
        }
        if (null == errorCode) {
            return new StringBuilder("{\"message\":\"").append(errorMsg).append("\"}").toString();
        } else {
            return new StringBuilder("{\"code\":").append(errorCode).append(",\"message\":\"").append(errorMsg).append("\"}").toString();
        }
    }

    private String templatePath(int code, ActionContext context) {
        ErrorTemplatePathResolver resolver = context.config().errorTemplatePathResolver();
        if (null == resolver) {
            resolver = new ErrorTemplatePathResolver.DefaultErrorTemplatePathResolver();
        }
        return resolver.resolve(code, context.accept());
    }

    private Template getTemplate(int statusCode, ActionContext context) {
        H.Format format = context.accept();
        String key = statusCode + "" + format;
        $.Var<Template> templateBag = templateCache.get(key);
        if (null == templateBag) {
            ViewManager vm = Act.viewManager();
            if (null == vm) {
                // unit testing
                return null;
            }
            context.templatePath(templatePath(statusCode, context));
            Template t = vm.load(context);
            if (null == t) {
                // try default one
                if (Act.isDev()) {
                    context.templatePath("/error/dev/errorPage." + context.accept().name());
                } else {
                    context.templatePath("/error/errorPage." + context.accept().name());
                }
                t = vm.load(context);
            }
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
