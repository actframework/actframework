package org.osgl.oms.handler.builtin;

import org.osgl.http.H;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.handler.RequestHandlerBase;
import org.osgl.oms.handler.builtin.controller.FastRequestHandler;

public class Redirect extends FastRequestHandler {

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
