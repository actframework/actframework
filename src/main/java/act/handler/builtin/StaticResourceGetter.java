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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import static org.osgl.http.H.Format.UNKNOWN;

/**
 * Unlike a {@link act.handler.builtin.StaticFileGetter}, the
 * `StaticResourceGetter` read resource from jar packages
 */
public class StaticResourceGetter extends FastRequestHandler {

    private static final String SEP = "/";

    private String base;
    private URL baseUrl;
    private boolean isFolder;

    private Set<URL> folders = new HashSet<>();

    public StaticResourceGetter(String base) {
        String path = base;
        if (!base.startsWith(SEP)) {
            path = SEP + base;
        }
        this.base = path;
        this.baseUrl = StaticFileGetter.class.getResource(path);
        this.isFolder = isFolder(this.baseUrl);
        E.illegalArgumentIf(null == this.baseUrl, "Cannot find base URL: %s", base);
    }

    @Override
    protected void releaseResources() {
    }

    @Override
    public void handle(ActionContext context) {
        context.handler(this);
        String path = context.paramVal(ParamNames.PATH);
        handle(path, context);
    }

    protected void handle(String path, ActionContext context) {
        try {
            URL target;
            H.Format fmt;
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
                    throw NotFound.get();
                }
            }
            if (preventFolderAccess(target, context)) {
                return;
            }
            fmt = StaticFileGetter.contentType(target.getPath());
            H.Response resp = context.resp();
            if (UNKNOWN != fmt) {
                resp.contentType(fmt.contentType());
            }
            try {
                IO.copy(target.openStream(), resp.outputStream());
            } catch (NullPointerException e) {
                // this is caused by accessing folder inside jar URL
                folders.add(target);
                AlwaysForbidden.INSTANCE.handle(context);
            }
        } catch (IOException e) {
            App.logger.warn(e, "Error servicing static resource request");
            throw NotFound.get();
        }
    }

    private boolean preventFolderAccess(URL target, ActionContext context) {
        if (folders.contains(target)) {
            AlwaysForbidden.INSTANCE.handle(context);
            return true;
        }
        if (isFolder(target)) {
            folders.add(target);
            AlwaysForbidden.INSTANCE.handle(context);
            return true;
        }
        return false;
    }

    private boolean isFolder(URL target) {
        if ("file".equals(target.getProtocol())) {
            File file = new File(target.getFile());
            return file.isDirectory();
        }
        return false;
    }

    @Override
    public boolean supportPartialPath() {
        return isFolder;
    }

    @Override
    public String toString() {
        return baseUrl.toString();
    }
}
