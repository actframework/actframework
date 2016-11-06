package act.handler.builtin;

import act.app.ActionContext;
import act.app.App;
import act.controller.ParamNames;
import act.handler.builtin.controller.FastRequestHandler;
import org.osgl.http.H;
import org.osgl.mvc.result.NotFound;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.IOException;
import java.net.URL;

import static org.osgl.http.H.Format.UNKNOWN;

/**
 * Unlike a {@link act.handler.builtin.StaticFileGetter}, the
 * `StaticResourceGetter` read resource from jar packages
 */
public class StaticResourceGetter extends FastRequestHandler {

    private static final String SEP = "/";

    private String base;
    private URL baseUrl;

    public StaticResourceGetter(String base) {
        String path = base;
        if (!base.startsWith(SEP)) {
            path = SEP + base;
        }
        this.base = path;
        this.baseUrl = StaticFileGetter.class.getResource(path);
        E.illegalArgumentIf(null == this.baseUrl, "Cannot find base URL");
    }

    @Override
    protected void releaseResources() {
    }

    @Override
    public void handle(ActionContext context) {
        context.handler(this);
        String path = context.paramVal(ParamNames.PATH);
        try {
            URL target;
            H.Format fmt = UNKNOWN;
            if (S.blank(path)) {
                target = baseUrl;
            } else {
                StringBuilder sb = S.builder(base);
                if (base.endsWith(SEP) || path.startsWith(SEP)) {
                    sb.append(path);
                } else {
                    sb.append(SEP).append(path);
                }
                target = StaticFileGetter.class.getResource(sb.toString());
                if (null == target) {
                    throw NotFound.INSTANCE;
                }
            }
            fmt = StaticFileGetter.contentType(base);
            H.Response resp = context.resp();
            if (UNKNOWN != fmt) {
                resp.contentType(fmt.contentType());
            }
            IO.copy(target.openStream(), resp.outputStream());
        } catch (IOException e) {
            App.logger.warn(e, "Error servicing static resource request");
            throw NotFound.INSTANCE;
        }
    }

    @Override
    public boolean supportPartialPath() {
    return true;
    }

    @Override
    public String toString() {
        return baseUrl.toString();
    }
}
