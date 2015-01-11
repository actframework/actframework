package org.osgl.mvc.server.action.builtin;

import org.osgl.http.H;
import org.osgl.mvc.server.AppContext;
import org.osgl.mvc.server.action.ActionHandlerBase;

public class Redirect extends ActionHandlerBase {

    private String url;

    public Redirect(String url) {
        this.url = url;
    }

    @Override
    public void invoke(AppContext context) {
        H.Response resp = context.resp();
        resp.status(H.Status.MOVED_PERMANENTLY);
        resp.writeContent(url);
    }
}
