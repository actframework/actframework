package act.handler.builtin;

import act.app.ActionContext;
import act.handler.builtin.controller.FastRequestHandler;
import org.osgl.http.H;
import org.osgl.util.S;

public class Echo extends FastRequestHandler {

    private String msg;
    private String contentType;

    public Echo(String msg) {
        this(msg, H.Format.TXT.contentType());
    }

    public Echo(String msg, String contentType) {
        this.msg = msg;
        this.contentType = contentType;
    }

    @Override
    public void handle(ActionContext context) {
        H.Response resp = context.resp();
        if (S.notBlank(contentType)) {
            resp.contentType(contentType);
        }
        resp.writeContent(msg);
    }

    @Override
    public String toString() {
        return "echo: " + msg;
    }
}
