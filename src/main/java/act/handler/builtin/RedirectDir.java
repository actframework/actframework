package act.handler.builtin;

import act.app.ActionContext;
import act.controller.ParamNames;
import act.handler.ExpressHandler;
import act.handler.builtin.controller.FastRequestHandler;
import org.osgl.http.H;
import org.osgl.util.S;

public class RedirectDir extends FastRequestHandler implements ExpressHandler {

    private String url;

    public RedirectDir(String url) {
        StringBuilder sb = S.builder(url);
        if (!url.endsWith("/")) {
            sb.append("/");
        }
        this.url = sb.toString();
    }

    @Override
    public void handle(ActionContext context) {
        H.Response resp = context.resp();
        if (context.isAjax()) {
            resp.status(H.Status.FOUND_AJAX);
        } else {
            resp.status(H.Status.MOVED_PERMANENTLY);
        }
        String path = context.paramVal(ParamNames.PATH);
        resp.header(H.Header.Names.LOCATION, url + path);
    }

    @Override
    public boolean supportPartialPath() {
        return true;
    }

    @Override
    public String toString() {
        return "redirect: " + url;
    }
}
