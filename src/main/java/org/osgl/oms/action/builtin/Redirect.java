package org.osgl.oms.action.builtin;

import org.osgl.http.H;
import org.osgl.oms.AppContext;
import org.osgl.oms.action.ActionHandlerBase;

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
