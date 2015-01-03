package org.osgl.mvc.server.action;

import org.osgl.http.H;
import org.osgl.mvc.server.AppContext;
import org.osgl.util.S;

public class Echo extends ActionInvokerBase {

    private String msg;
    private String contentType;

    public Echo(String msg) {
        this(msg, H.Format.html.toContentType());
    }

    public Echo(String msg, String contentType) {
        this.msg = msg;
        this.contentType = contentType;
    }

    @Override
    public void invoke(AppContext context) {
        H.Response resp = context.resp();
        if (S.notEmpty(contentType)) {
            resp.contentType(contentType);
        }
        resp.writeContent(msg.toString());
    }
}
