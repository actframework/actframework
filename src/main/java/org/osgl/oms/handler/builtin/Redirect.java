package org.osgl.oms.handler.builtin;

import org.osgl.http.H;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.handler.RequestHandlerBase;

public class Redirect extends RequestHandlerBase {

    private String url;

    public Redirect(String url) {
        this.url = url;
    }

    @Override
    public void handle(AppContext context) {
        H.Response resp = context.resp();
        resp.status(H.Status.MOVED_PERMANENTLY);
        resp.writeContent(url);
    }
}
