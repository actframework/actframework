package act.util;

import act.app.ActionContext;
import act.app.App;
import act.app.util.NamedPort;
import act.conf.AppConfig;
import act.route.Router;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.mvc.result.Redirect;
import org.osgl.mvc.result.Result;
import org.osgl.util.S;

import java.util.HashMap;
import java.util.Map;

/**
 * When authentication is required but missing, redirect the user to
 * {@link act.conf.AppConfigKey#LOGIN_URL}
 */
public class RedirectToLoginUrl implements MissingAuthenticationHandler {

    private volatile Result R = null;
    private volatile Result R_AJAX = null;
    private Map<String, Result> resultMap = new HashMap<>();
    private Map<String, Result> ajaxResultMap = new HashMap<>();

    public RedirectToLoginUrl() {
        App app = App.instance();
        AppConfig<?> config = app.config();
        String loginUrl = config.loginUrl();
        String ajaxLoginUrl = config.ajaxLoginUrl();

        Router router = app.router();
        $.Var<Result> result = $.var(), ajaxResult = $.var();
        findResults(loginUrl, ajaxLoginUrl, result, ajaxResult, router);
        R = result.get();
        R_AJAX = result.get();
        resultMap.put("", R);
        ajaxResultMap.put("", R_AJAX);
        for (NamedPort port: config.namedPorts()) {
            Router routerX = app.router(port);
            findResults(loginUrl, ajaxLoginUrl, result, ajaxResult, routerX);
            resultMap.put(port.name(), result.get());
            ajaxResultMap.put(port.name(), ajaxResult.get());
        }
    }

    @Override
    public Result result(ActionContext context) {
        if (context.isAjax()) {
            return _ajaxResult(context);
        } else {
            return _result(context);
        }
    }

    private Result _ajaxResult(ActionContext context) {
        String portId = context.router().portId();
        return S.blank(portId) ? R_AJAX : ajaxResultMap.get(portId);
    }

    private Result _result(ActionContext context) {
        String portId = context.router().portId();
        return S.blank(portId) ? R : resultMap.get(portId);
    }

    private void findResults(String loginUrl, String ajaxLoginUrl, $.Var<Result> result, $.Var<Result> ajaxResult, Router router) {
        result.set(hasRouteTo(loginUrl, router) ? new Redirect(loginUrl) : ReturnUnauthorized.result());
        ajaxResult.set(S.eq(ajaxLoginUrl, loginUrl) ? R : hasRouteTo(ajaxLoginUrl, router) ? new Redirect(ajaxLoginUrl) : ReturnUnauthorized.result());
    }

    private boolean hasRouteTo(String url, Router router) {
        return null != router.findStaticGetHandler(url);
    }
}
