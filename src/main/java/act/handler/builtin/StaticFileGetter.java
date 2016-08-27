package act.handler.builtin;

import act.app.ActionContext;
import act.app.App;
import act.controller.ParamNames;
import act.handler.builtin.controller.FastRequestHandler;
import org.osgl.http.H;
import org.osgl.util.E;
import org.osgl.util.FastStr;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;

public class StaticFileGetter extends FastRequestHandler {
    private File base;

    public StaticFileGetter(String base, App app) {
        E.NPE(base);
        this.base = app.file(base);
    }

    public StaticFileGetter(File base) {
        E.NPE(base);
        this.base = base;
    }

    @Override
    protected void releaseResources() {
        base = null;
    }

    @Override
    public void handle(ActionContext context) {
        context.handler(this);
        File file = base;
        if (!file.exists()) {
            AlwaysNotFound.INSTANCE.handle(context);
            return;
        }
        H.Format fmt;
        if (base.isDirectory()) {
            String path = context.paramVal(ParamNames.PATH);
            if (S.blank(path)) {
                AlwaysBadRequest.INSTANCE.handle(context);
                return;
            }
            fmt = contentType(path);
            file = new File(base, path);
            if (!file.exists()) {
                AlwaysNotFound.INSTANCE.handle(context);
                return;
            }
            if (!file.canRead()) {
                AlwaysForbidden.INSTANCE.handle(context);
                return;
            }
        } else {
            fmt = contentType(file.getPath());
        }
        InputStream is = new BufferedInputStream(IO.is(file));
        H.Response resp = context.resp();
        if (null != fmt && H.Format.UNKNOWN != fmt) {
            resp.contentType(fmt.contentType());
        }
        IO.copy(is, context.resp().outputStream());
    }

    // for unit test
    public File base() {
        return base;
    }

    private H.Format contentType(String path) {
        FastStr s = FastStr.unsafeOf(path).afterLast('.');
        return H.Format.of(s.toString());
    }

    protected InputStream inputStream(String path, ActionContext context) {
        return context.app().classLoader().getResourceAsStream(path);
    }

    @Override
    public boolean supportPartialPath() {
    return base.isDirectory();
    }

    @Override
    public String toString() {
        boolean dir = supportPartialPath();
        return "file: " + (dir ? base().getPath() + "/**" : base().getPath());
    }
}
