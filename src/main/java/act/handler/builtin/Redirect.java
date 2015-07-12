package act.handler.builtin;

import act.app.AppContext;
import act.handler.builtin.controller.FastRequestHandler;
import org.osgl.http.H;

public class Redirect extends FastRequestHandler {

    private String url;

    public Redirect(String url) {
        this.url = url;
    }

    @Override
    public void handle(AppContext context) {
        H.Response resp = context.resp();
        resp.status(H.Status.MOVED_PERMANENTLY);
        resp.header(H.Header.Names.LOCATION, url);
    }

    @Override
    public String toString() {
        return "redirect: " + url;
    }
}
