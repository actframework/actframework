package org.osgl.oms.handler.builtin;

import org.osgl.http.H;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.handler.RequestHandlerBase;
import org.osgl.util.S;

public class Echo extends RequestHandlerBase {

    private String msg;
    private String contentType;

    public Echo(String msg) {
        this(msg, H.Format.txt.toContentType());
    }

    public Echo(String msg, String contentType) {
        this.msg = msg;
        this.contentType = contentType;
    }

    @Override
    public void handle(AppContext context) {
        H.Response resp = context.resp();
        if (S.notBlank(contentType)) {
            resp.contentType(contentType);
        }
        resp.writeContent(msg);
    }
}
