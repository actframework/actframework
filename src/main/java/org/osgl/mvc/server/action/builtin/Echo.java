package org.osgl.mvc.server.action.builtin;

import org.osgl.http.H;
import org.osgl.mvc.server.AppContext;
import org.osgl.mvc.server.action.ActionHandlerBase;
import org.osgl.util.S;

public class Echo extends ActionHandlerBase {

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
    public void invoke(AppContext context) {
        H.Response resp = context.resp();
        if (S.notBlank(contentType)) {
            resp.contentType(contentType);
        }
        resp.writeContent(msg);
    }
}
