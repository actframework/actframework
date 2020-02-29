package act.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static org.osgl.http.H.Format.*;

import act.Act;
import act.app.ActionContext;
import act.controller.Controller;
import act.view.Template;
import act.view.ViewManager;
import org.apache.commons.lang3.StringEscapeUtils;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.mvc.ErrorPageRenderer;
import org.osgl.mvc.MvcConfig;
import org.osgl.mvc.result.ErrorResult;
import org.osgl.util.S;

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
        fixRequestAcceptFormat(context);
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
            if (H.Format.JSON.isSameTypeWith(accept)) {
                return jsonContent(error, errorCode, errorMsg);
            } else if (HTML.isSameTypeWith(accept)) {
                String header = S.concat("HTTP/1.1 ", Integer.toString(statusCode), " ", errorMsg);
                return S.concat("<!DOCTYPE html><html><head><meta charset='utf-8'><title>"
                        , header
                        , "</title></head><body><h1>"
                        , header, "</h1></body></html>");
            } else if (H.Format.XML.isSameTypeWith(accept)) {
                S.Buffer sb = S.buffer();
                sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><error>");
                if (null != errorCode) {
                    sb.append("<code>").append(errorCode).append("</code");
                }
                sb.append("<message>").append(errorMsg).append("</message></error>");
                return sb.toString();
            } else if (CSV.isSameTypeWith(accept)) {
                if (null == errorCode) {
                    return S.concat("message\n", errorMsg);
                } else {
                    return S.concat("code,message\n", Integer.toString(errorCode), ",", errorMsg);
                }
            } else if (H.Format.TXT.isSameTypeWith(accept)) {
                return null == errorCode ? errorMsg : S.concat(Integer.toString(errorCode), " ", errorMsg);
            } else {
                // Unknown accept format
                return "";
            }
        }
        if (HTML == context.accept()) {
            String header = S.concat("HTTP/1.1 ", Integer.toString(statusCode), " ", error.getMessage());

            context.renderArg("header", header);
        }
        context.renderArg(ARG_ERROR, error);
        return t.render(context);
    }

    private void fixRequestAcceptFormat(ActionContext context) {
        H.Request req = context.req();
        if (null != req && !isAcceptGoodForErrorPage(req.accept())) {
            req.accept(H.Format.JSON);
        }
    }

    private boolean isAcceptGoodForErrorPage(H.Format fmt) {
        return fmt.isSameTypeWithAny(HTML, CSV, JSON, XML);
    }

    private String jsonContent(ErrorResult error, Integer errorCode, String errorMsg) {
        Object payload = error.attachment();
        if (null != payload) {
            return com.alibaba.fastjson.JSON.toJSONString(payload);
        }
        errorMsg = StringEscapeUtils.escapeJson(errorMsg);
        if (null == errorCode) {
            return S.concat("{\"ts\":", $.ms(),  ",\"message\":\"", errorMsg, "\"}");
        } else {
            return S.concat("{\"ts\":", $.ms(), ",\"code\":", S.string(errorCode), ",\"message\":\"", errorMsg, "\"}");
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
